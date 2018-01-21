package com.example.chaosruler.msa_manager.services

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyProperties
import android.util.Log
import com.example.chaosruler.msa_manager.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec


@Suppress("unused")
/**
 * a Singleton object (only one instance in runtime) responsible for encryptiing and decrypting
 * data stored in local database and local android machine
 * https://en.wikipedia.org/wiki/Advanced_Encryption_Standard
 * AES 256bit was chosen for encryption protocol
 * @author Chaosruler972
 * @constructor no construction needed, though key Generation is a must before each and every encryption or decryption
 * @sample generate_key(baseContext)
 * @sample encrypt(a.toByteArray())
 */
object encryption
{
    private var secretKey:SecretKey? = null
    private var iv = ByteArray(16)
    /**
     * this function is responsible for generatoin an AES key from the keystore per encryption/decryption, and refreshing the current one
     * @param context a base Context, must not be null, for keyStore access
     */
    fun generate_key(context: Context)
    {
        iv = ByteArray(16)
        if(secretKey==null)
        {
            val store = KeyStore.getInstance(KeyStore.getDefaultType())
            store.load(null)
            val alias = context.getString(R.string.ENC_KEY)
            if (store.containsAlias(alias))
            {
                secretKey = (store.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
            }
            else
            {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
                keyGen.init(256)
                secretKey = keyGen.generateKey()
                if(secretKey==null)
                    Log.d("Key", "Is null")
                val skEntry = KeyStore.SecretKeyEntry(secretKey)
                store.setEntry(alias, skEntry,KeyStore.PasswordProtection(alias.toCharArray()))

            }
        }
    }

    /**
     * This function encrypts a byteArray and returns an encrypted form of that byteArray
     * Must call generate_key() before this function
     * @param the byteArray to encrypt
     * @return the encrypted form of that bytearray
     */
    @SuppressLint("GetInstance")
    fun encrypt(a: ByteArray): ByteArray
    {

        val c = Cipher.getInstance("AES")
        c.init(Cipher.ENCRYPT_MODE, secretKey!!,IvParameterSpec(iv))
        return c.doFinal(a)
    }

    /**
     * This function decrypts a byteArray
     * Must call generate_key() before this function
     * @param an encrypted byteArray
     * @return a decrypted form of that bytearray
     */
    @SuppressLint("GetInstance")
    fun decrypt(a:ByteArray): ByteArray
    {
        val c = Cipher.getInstance("AES")
        c.init(Cipher.DECRYPT_MODE, secretKey!!,IvParameterSpec(iv))
        return c.doFinal(a)
    }

}