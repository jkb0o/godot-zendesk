def can_build(platform):
    return platform == "android"


def configure(env):
    if env['platform'] == 'android':
        env.android_add_maven_repository("url 'https://zendesk.jfrog.io/zendesk/repo'")
        env.android_add_dependency("compile 'com.zendesk:sdk:1.10.1.1'")
        env.android_add_dependency("compile 'com.zendesk:sdk-providers:1.10.1.1'")
        env.android_add_java_dir("android")
        #env.android_add_to_manifest("AndroidManifestChunk.xml")
