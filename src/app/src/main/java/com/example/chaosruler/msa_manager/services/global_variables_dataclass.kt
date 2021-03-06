package com.example.chaosruler.msa_manager.services

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.preference.PreferenceManager
import com.example.chaosruler.msa_manager.R
import com.example.chaosruler.msa_manager.SQLITE_helpers.sync_table.*
import com.example.chaosruler.msa_manager.SQLITE_helpers.user_database_helper
import com.example.chaosruler.msa_manager.activies.MainActivity
import com.example.chaosruler.msa_manager.object_types.*
import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * a Singleton like objec that holds data true across all the activities such as settings and databases along with functions usabele across activities
 * @author Chaosruler972
 * @constructor an empty constructor since this is a singleton
 */
object global_variables_dataclass
{
    /**
     * A boolean value that tells me if database is stored locally or not, if it's not all calls are remote
     * @author Chaosruler972
     */
    var isLocal: Boolean = true
    /**
     * a boolean value that tells me if I should load any database values, aka GUI mode (in case we don't)
     * @author Chaosruler972
     */
    var GUI_MODE: Boolean = false

    /**
     * The projid chosen by the user is stored here from the MainActivity, throughout all the flow it's retrieved from here
     * @author Chaosruler972
     */
    var projid: String? = null

    /**
     * The chosen flat (if any) is stored here for specific activities that requires it
     * @author Chaosruler972
     */
    var flat: String? = null
    /**
     * The chosen floor (if any) is stored here for specific activiies that require it
     * @author Chaosruler972
     */
    var floor: String? = null
    /**
     * a integer value for the aforementioned specific activies that tell me which activity was it
     * for flow integration purposes only
     * @author Chaosruler972
     */
    var floor_moving_to: Int = 0

    /**
     * A locally loaded database of db_big
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_BIG: local_big_table_helper? = null
    /**
     * A locally loaded vector of db_big when loaded to memory
     * @author Chaosruler972
     */
    var db_big_vec : Vector<big_table_data> = Vector()
    @SuppressLint("StaticFieldLeak")
    /**
     * A locally loaded database of project
     * @author Chaosruler972
     */
    var DB_project: local_projects_table_helper? = null
    /**
     * A locally loaded vector of project when loaded to memory
     * @author Chaosruler972
     */
    var db_project_vec : Vector<project_data> = Vector()
    /**
     * A locally loaded database of OPR
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_OPR: local_OPR_table_helper? = null
    /**
     * A locally loaded vector of opr when loaded to memory
     * @author Chaosruler972
     */
    var db_opr_vec : Vector<opr_data> = Vector()

    /**
     * A locally loaded database of vendor
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_VENDOR: local_vendor_table_helper? = null
    /**
     * A locally loaded vector of vendor when loaded to memory
     * @author Chaosruler972
     */
    var db_vendor_vec : Vector<vendor_data> = Vector()

    /**
     * A locally loaded database of salprojluz
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_SALPROJ: local_salprojluz_table_helper? = null
    /**
     * A locally loaded vector of salprojluz when loaded to memory
     * @author Chaosruler972
     */
    var db_salproj_vec : Vector<salprojluz_data> = Vector()


    /**
     * A locally loaded database of salprojtakala
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_SALPROJTAKALA: local_salprojtakala_table_helper? = null
    /**
     * A locally loaded vector of takala data when loaded to memory
     * @author Chaosruler972
     */
    var db_salprojtakala_vec: Vector<takala_data> = Vector()

    // Following were removed from production
    /**
     * A locally loaded database of inventory
     * Removed from use
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_INVENTORY: local_inventory_table_helper? = null
    /**
     * A locally loaded vector of inventory when loaded to memory
     * removed from use
     * @author Chaosruler972
     */
    var db_inv_vec : Vector<inventory_data> = Vector()

    /**
     * A locally loaded database of users
     * @author Chaosruler972
     */
    @SuppressLint("StaticFieldLeak")
    var DB_USERS : user_database_helper? = null
    // Users is loaded only once, therefore we don't need a vector to store that

    /**
     * Inits all the database with an instance that can be called from all the objects
     * @author Chaosruler972
     * @param context a baseContext to work with
     */
    fun init_dbs(context: Context) {
        isLocal = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.local_or_not), true)
        GUI_MODE = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.gui_mode_key), false)

        DB_BIG = local_big_table_helper(context)
//        DB_INVENTORY = local_inventory_table_helper(context)
        DB_OPR = local_OPR_table_helper(context)
        DB_VENDOR = local_vendor_table_helper(context)
        DB_project = local_projects_table_helper(context)
        DB_USERS = user_database_helper(context)
        DB_SALPROJ = local_salprojluz_table_helper(context)
        DB_SALPROJTAKALA = local_salprojtakala_table_helper(context)
    }

    /**
     * a bridge function to report syncing is done to main activity
     * @author Chaosruler972
     */
    fun report_to_Main_Activity_Thread_syncing_is_done() {
        MainActivity.service_sync_done = true
    }

    /**
     * Encrypts and decrypts with Base64 encoding for hashing and security
     * also adds the functionality of AES encryption using the encryption class
     * @see encryption
     * @author Chaosruler972
     * @param a a bytearray to encrypt/decrypt
     * @param key a bytearray to use for decryp/encrypt key
     * @param con a base Context to work with
     * @param flag true = decryption, false= encryption
     */
    @SuppressLint("GetInstance")
    fun xorWithKey(a: ByteArray, @Suppress("UNUSED_PARAMETER") key: ByteArray, flag: Boolean, con: Context): ByteArray {
        encryption.generate_key(con)
        return if (flag)
            encryption.decrypt(a)
        else
            encryption.encrypt(a)
        //return new_a
    }

    /**
     * Gets this device WiFi MAC ID for device identification, COULD BE SPOOFED
     * @author Chaosruler972
     * @param con the basecontext to work with
     * @return the device ID
     */
    @SuppressLint("WifiManagerPotentialLeak", "HardwareIds")
    fun get_device_id(con: Context): String {
        val wifiManager = con.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wInfo = wifiManager.connectionInfo
        return wInfo.macAddress
    }

    @Suppress("unused")
            /**
     * Converts a string to hebrew (unicode)
     * @author Chaosruler972
     * @param str the string to convertStrEncoding
     * @return a converted string with UTF8 this time
     */
    fun to_hebrew_unicode(str: String): String {

        //WINDOWS-1255
        // UTF-8
        // Log.d("Char unicoded", toHex(str))
        val charSet = "UTF-8"
        // Log.d("Result after decode:",String(str.toByteArray(charset = Charset.forName(charSet)),Charset.forName(charSet)))
        return String(str.toByteArray(charset = Charset.forName(charSet)), Charset.forName(charSet))
    }

    /**
     * Finds the charset of a string
     * @author Chaosruler972
     * @param value the string to scan
     * @param charsets the list of possible charsets to scan
     * @return the charset that holds right to this string
     */
    @Suppress("unused")
    private fun charset(value: String, charsets: Array<String>): String {
        val probe = StandardCharsets.UTF_8.name()
        for (c in charsets) {
            val charset = Charset.forName(c)
            if (charset != null) {
                if (value == convertStrEncoding(convertStrEncoding(value, charset.name(), probe), probe, charset.name())) {
                    return c
                }
            }
        }
        return StandardCharsets.UTF_8.name()
    }

    /**
     * Converts a string from encoding a to encoding B
     * @author Chaosruler972
     * @param value the string to convertStrEncoding
     * @param fromEncoding the current encoding
     * @param toEncoding the desired encoding
     * @return the resulting string after convertion
     */
    private fun convertStrEncoding(value: String, fromEncoding: String, toEncoding: String): String = String(value.toByteArray(charset(fromEncoding)), Charset.forName(toEncoding))

    /**
     * Converts string to its hexadecimal representation (per byte)
     * @author Chaosruler972
     * @param arg the string to convertStrEncoding
     * @return the converted string to hexadecimal format
     */
    @Suppress("unused")
    fun toHex(arg: String): String {
        return String.format("%040x", BigInteger(1, arg.toByteArray()/*YOUR_CHARSET?*/))
    }

    /**
     * converts time from date.time.time to MSSQL time object string
     * @author Chaosruler972
     * @param time time form date.time.time
     * @return String of MSSQL date time
     */
    fun time_to_mssql_time(time: Long) : String
    {
        return "dateadd(s,${time},'19700101 00:00:00:000')"
    }

}