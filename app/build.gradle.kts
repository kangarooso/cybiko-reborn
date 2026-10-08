plugins { id("com.android.application") }
android { namespace = "com.cybikoreborn"; compileSdk = 35
 defaultConfig { applicationId = "com.cybikoreborn"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "0.2.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation(project(":cybiko-core")) }
