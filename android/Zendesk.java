package org.godotengine.godot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.app.PendingIntent;
import android.os.SystemClock;
import android.content.Intent;
import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.godot.game.R;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import java.util.Currency;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.math.BigDecimal;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.sdk.model.access.AnonymousIdentity;
import com.zendesk.sdk.model.access.Identity;
import com.zendesk.sdk.support.SupportActivity;
import com.zendesk.sdk.model.request.RequestUpdates;
import com.zendesk.sdk.requests.RequestActivity;
import com.zendesk.service.ZendeskCallback;
import com.zendesk.service.ErrorResponse;
import com.zendesk.sdk.network.RequestProvider;
import com.zendesk.sdk.model.request.Request;
import com.zendesk.sdk.model.request.CommentsResponse;
import com.zendesk.sdk.model.request.CommentResponse;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.zendesk.sdk.requests.ViewRequestActivity;
import com.zendesk.sdk.feedback.BaseZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.model.request.CustomField;

public class Zendesk extends Godot.SingletonBase {
    Activity activity;
    Application app;
    Boolean loggedIn = false;
    String[] updates;
    int commentsCount = -1;
    int requestsCount = -1;
    String lastRequestId = "";
    final RequestProvider requestProvider;// = ZendeskConfig.INSTANCE.provider().requestProvider();
    ZendeskFeedbackConfiguration configuration;
    String ticketTitle;
    String ticketInfo; 
    List<CustomField> fields;



    static public Godot.SingletonBase initialize(Activity p_activity) {
        return new Zendesk(p_activity);
    }


    public Zendesk(Activity p_activity) {
        registerClass("Zendesk", new String[]{
            "login", 
            "showSupport", 
            "showRequests", 
            "hasUpdates", 
            "requestUpdates",
            "getCommentsCount",
            "requestCommentsCount",
            "setTicketInfo",
            "setTicketTitle",
            "addField",
            "setTicketFormId",
            "getVersionName"
        });
        activity = p_activity;
        app = p_activity.getApplication();
        String url = GodotLib.getGlobal("Zendesk/url");
        String appId = GodotLib.getGlobal("Zendesk/application_id");
        String clientId = GodotLib.getGlobal("Zendesk/oauth_client_id");
        fields = new ArrayList<CustomField>();

        updates = new String[0];
        ZendeskConfig.INSTANCE.init(app, url, appId, clientId);
        requestProvider = ZendeskConfig.INSTANCE.provider().requestProvider();
        configuration = new BaseZendeskFeedbackConfiguration() {
            @Override
            public String getRequestSubject() {
                return ticketTitle;
            }

            @Override
            public String getAdditionalInfo() {
                return ticketInfo;
            }

            @Override
            public List<String> getTags(){
                List<String> res = new ArrayList<String>();
                res.add("summer_tales");
                return res;
            }
        };
                

        
    }

    public void login(String name, String email){
        if (loggedIn) return;
        loggedIn = true;
        Identity identity = new AnonymousIdentity.Builder()
            .withNameIdentifier(name)
            .withEmailIdentifier(email)
            .build();
        ZendeskConfig.INSTANCE.setIdentity(identity);
        requestCommentsCount();
    }

    public void requestUpdates(){
        requestProvider.getUpdatesForDevice(new ZendeskCallback<RequestUpdates>() {
            @Override
            public void onSuccess(RequestUpdates requestUpdates) {

                if (requestUpdates != null && requestUpdates.hasUpdates()) {
                    Set<String> updatedRequestIDs = requestUpdates.getRequestsWithUpdates().keySet();
                    updates = updatedRequestIDs.toArray(new String[updatedRequestIDs.size()]);

                    //for (String id : updatedRequestIDs) {
                    //    Logger.d(LOG_TAG, "Request %s has %d updates",
                    //            id, requestUpdates.getRequestsWithUpdates().get(id));
                    //}
                } else {
                    updates = new String[0];
                }
            }
            @Override
            public void onError(ErrorResponse errorResponse) {
                // handle error
            }
        });
    }

    public void requestCommentsCount(){
        ZendeskConfig.INSTANCE.provider().requestProvider().getAllRequests(new ZendeskCallback<List<Request>>(){
            @Override
            public void onSuccess(List<Request> requests) {
                commentsCount = 0;
                requestsCount = 0;
                //final Dictionary<String, Long> reqToAuthor =  new Dictionary<String, Long>();
                for (Request request:requests){
                    requestsCount += 1;
                    final long requesterId = request.getRequesterId();
                    lastRequestId = request.getId();
                    //reqToAuthor[lastRequestId] = request.getRequesterId();
                    ZendeskConfig.INSTANCE.provider().requestProvider().getComments(request.getId(), new ZendeskCallback<CommentsResponse>(){
                        @Override
                        public void onSuccess(CommentsResponse resp){
                            for (CommentResponse comment: resp.getComments()){
                                if (comment.getAuthorId() != requesterId){
                                    commentsCount += 1;
                                }
                            }
                        }
                        @Override
                        public void onError(ErrorResponse errorResponse){
                            
                        }
                    });
                    
                }
            }
            @Override
            public void onError(ErrorResponse errorResponse){
            }
        });
    }

    public int getCommentsCount(){
        return commentsCount;
    }

    public void showSupport(){
        new SupportActivity.Builder().show(activity);
    }

    public void showRequests(){
        if (requestsCount > 0){
            Intent intent = new Intent(activity, ViewRequestActivity.class);
            intent.putExtra(ViewRequestActivity.EXTRA_SUBJECT, "Summer Tales Support");
            intent.putExtra(ViewRequestActivity.EXTRA_REQUEST_ID, lastRequestId);
            activity.startActivity(intent);
        } else {
            ContactZendeskActivity.startActivity(activity, configuration);
            //ContactZendeskActivity.startActivity(activity, null);
        }
    }

    public int hasUpdates(){
        return updates.length > 0 ? 1 : 0;
    }

    public void setTicketTitle(String title){
        ticketTitle = title;
    }

    public void setTicketInfo(String info){
        ticketInfo = info;
    }

    public void addField(int id, String value){
        fields.add(new CustomField((long)id, value));
        ZendeskConfig.INSTANCE.setCustomFields(fields);
    }

    public void setTicketFormId(int id){
        ZendeskConfig.INSTANCE.setTicketFormId((long)id);
    }

    public String getVersionName(){
        try {
            PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }

}
