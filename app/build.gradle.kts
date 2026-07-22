plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.guilherme.anotaplus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guilherme.anotaplus"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"https://anotaplus-backend.onrender.com/\"")
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"666199767955-c02f82u6vv8sg1o8a0i3uhd65h38k9j7.apps.googleusercontent.com\""
        )

        // Assinatura PRO (Google Play Billing): id do produto de assinatura
        // que precisa ser cadastrado no Play Console com esse EXATO id
        // antes de qualquer compra funcionar (ainda não existe — ver
        // PROJETO.md).
        buildConfigField("String", "SUBSCRIPTION_PRODUCT_ID", "\"anotaplus_pro_mensal\"")

        // AdMob (anúncios do plano Free): IDs de TESTE oficiais do Google
        // (developers.google.com/admob/android/test-ads) — seguros de
        // publicar, mas só mostram anúncio de teste, sem gerar receita
        // nenhuma. Trocar pelos IDs reais assim que a conta AdMob existir.
        manifestPlaceholders["adMobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "AD_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        buildConfigField("String", "AD_INTERSTITIAL_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
    }

    signingConfigs {
        getByName("debug") {
            // Keystore de debug fixo e commitado (não é segredo — é só pra
            // ter um SHA-1 estável entre builds do GitHub Actions, já que
            // cada runner é efêmero e geraria um keystore de debug
            // diferente a cada vez, quebrando o Google Sign-In).
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    // ProcessLifecycleOwner — detecta quando o app INTEIRO (não só uma
    // activity) sai de primeiro plano, pra derrubar a task (ver
    // AnotaPlusApplication.kt).
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")

    // Room (armazenamento local)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Login com Google (Credential Manager, API atual — não precisa de google-services.json)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Cliente HTTP pro backend (NestJS)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Backup na nuvem em segundo plano — precisa sobreviver mesmo se a
    // activity que disparou o salvamento já tiver fechado (QuickCapture
    // sempre finish() logo em seguida), então uma coroutine presa ao
    // lifecycle da activity não serve.
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Assinatura PRO (Google Play Billing) e anúncios do plano Free (AdMob)
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.gms:play-services-ads:23.5.0")

    // Gráfico de saldo do Financeiro — via JitPack (ver settings.gradle.kts)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
