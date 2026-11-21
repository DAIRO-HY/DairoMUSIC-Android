import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "cn.dairo.music"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "cn.dairo.music"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "2.0.1"

        //DairoDFS的SDK只编译了这几种CPU的动态库,其他CPU(32位x86模拟器)上安装时直接提示不支持,不会启动就闪退
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    //签名信息不写在代码里:Jenkins等编译服务器用环境变量,本机用项目根目录的keystore.properties(不提交到git)
    //都没有时(例如clone开源代码编译),debug用默认的调试签名,release生成未签名的APK
    val signing = signingInfo()
    signingConfigs {
        if (signing != null) {
            create("releaseConfig") {
                storeFile = signing.storeFile
                storePassword = signing.storePassword
                keyAlias = signing.keyAlias
                keyPassword = signing.keyPassword
            }
        }
    }

    buildTypes {
        release {
            if (signing != null) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {

            //和正式版用同一个签名,调试时可以直接覆盖安装正式版
            if (signing != null) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.dairo.ui)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    //播放器:后台播放、通知栏和锁屏控制、视频画面
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui.compose)

    //DairoDFS的文件下载缓存SDK,编译方法见README;SDK通过JNA调用动态库
    implementation(files("libs/dairo-file-sdk-0.1.0.aar"))
    implementation("${libs.jna.get()}@aar")
}

/**
 * 签名信息
 */
class SigningInfo(val storeFile: File, val storePassword: String, val keyAlias: String, val keyPassword: String)

/**
 * 读取签名信息,优先使用环境变量,其次是项目根目录的keystore.properties,都没有时返回null
 *
 * 环境变量(Jenkins里用凭据注入):
 *   DAIRO_KEYSTORE_FILE      签名文件的路径
 *   DAIRO_KEYSTORE_PASSWORD  签名文件的密码
 *   DAIRO_KEY_ALIAS          密钥别名
 *   DAIRO_KEY_PASSWORD       密钥的密码
 *
 * keystore.properties(格式见keystore.properties.example):
 *   storeFile、storePassword、keyAlias、keyPassword,storeFile可以写相对于项目根目录的路径
 */
fun signingInfo(): SigningInfo? {
    val env = System.getenv()
    val envFile = env["DAIRO_KEYSTORE_FILE"]
    if (!envFile.isNullOrEmpty()) {
        return SigningInfo(
            rootProject.file(envFile),
            env["DAIRO_KEYSTORE_PASSWORD"] ?: error("缺少环境变量DAIRO_KEYSTORE_PASSWORD"),
            env["DAIRO_KEY_ALIAS"] ?: error("缺少环境变量DAIRO_KEY_ALIAS"),
            env["DAIRO_KEY_PASSWORD"] ?: error("缺少环境变量DAIRO_KEY_PASSWORD"),
        )
    }
    val file = rootProject.file("keystore.properties")
    if (!file.isFile) {
        return null
    }
    val properties = Properties().apply { file.inputStream().use { load(it) } }
    fun value(key: String) = properties.getProperty(key)?.takeIf { it.isNotEmpty() } ?: error("keystore.properties缺少$key")
    return SigningInfo(rootProject.file(value("storeFile")), value("storePassword"), value("keyAlias"), value("keyPassword"))
}
