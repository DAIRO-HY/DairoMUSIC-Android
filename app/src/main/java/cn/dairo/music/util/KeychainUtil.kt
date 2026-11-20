package cn.dairo.music.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import cn.dairo.ui.DairoUI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 加密存取,对应iOS的钥匙串,用来保存登录票据、设备标识等不能明文保存的数据
 * 用Android Keystore里的密钥加密之后保存到SharedPreferences,密钥不能导出,数据不会通过备份迁移到其他设备
 */
object KeychainUtil {

    /** 保存数据的SharedPreferences名 */
    private const val PREFERENCES_NAME = "keychain"

    /** Keystore里的密钥名 */
    private const val KEY_ALIAS = "DairoMUSIC_KEYCHAIN"

    /** GCM的IV长度 */
    private const val IV_SIZE = 12

    private val preferences by lazy {
        DairoUI.context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /** 读取 */
    fun read(key: String): String? {
        val value = preferences.getString(key, null) ?: return null
        return runCatching {
            val data = Base64.decode(value, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, data, 0, IV_SIZE))
            String(cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE))
        }.getOrNull()
    }

    /**
     * 保存,已经存在时覆盖
     * @return 是否保存成功
     */
    fun write(value: String, key: String): Boolean {
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val data = cipher.iv + cipher.doFinal(value.toByteArray())
            preferences.edit().putString(key, Base64.encodeToString(data, Base64.NO_WRAP)).apply()
        }.isSuccess
    }

    /** 删除 */
    fun delete(key: String) {
        preferences.edit().remove(key).apply()
    }

    /** 获取密钥,第一次使用时生成 */
    @Synchronized
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
