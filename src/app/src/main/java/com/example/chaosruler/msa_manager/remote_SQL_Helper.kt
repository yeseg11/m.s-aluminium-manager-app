package com.example.chaosruler.msa_manager

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Debug
import android.util.Log
import android.widget.Toast
import java.sql.*

import java.util.*
import kotlin.collections.HashMap

class remote_SQL_Helper()
{
    companion object
    {

        private lateinit var context:Context
        public lateinit var username: String
        private lateinit var password: String
        private var isvalid: Boolean = false
        private var connection: Connection? = null
        private lateinit var exception:SQLException
        fun getusername():String
                = username

        fun isValid():Boolean
                = isvalid
        fun getSQLException():SQLException
                = exception
        /*
            subroutine neccesery to connect to db, without this call, no DB operations can be done
         */
        fun Connect(con:Context,user: String, pass: String): Boolean
        {
            if (isvalid)
                return true
            context = con
            username = user
            password = pass
            try {
                Class.forName(context.getString(R.string.class_jtds_jdbc))
                var conn: Connection? = DriverManager.getConnection(
                        context.getString(R.string.REMOTE_CONNECT_STRING) + context.getString(R.string.REMOTE_IP_ADDR) + context.getString(R.string.REMOTE_CONNECT_OPTIONS)
                        , username,
                        password)
                if (conn != null)
                {
                    isvalid = true
                    connection = conn
                }
            }
            catch (e: SQLException)
            {
                e.printStackTrace()
                isvalid = false
                exception = e

            }
            return isvalid
        }


        /*
            subroutine that gets as parameters an entire table and converts it into Hashmap vector, which is later can be converted to string, sql is select * form table
         */
        fun get_all_table(db: String, table: String): Vector<HashMap<String, String>>? {
            try
            {
                connection!!.isReadOnly
            }
            catch (e:SQLException)
            {
                if(e.errorCode==0)
                {
                    ReConnect()
                }
            }
            catch (e:KotlinNullPointerException)
            {
                ReConnect()
            }
            if(!isvalid)
                return null
            try
            {
                var vector: Vector<HashMap<String, String>> = Vector()
                var lock = java.lang.Object()
                AsyncTask.execute(
                {
                    var rs:ResultSet?
                    rs = null
                    try
                    {
                        rs = connection!!.createStatement().executeQuery("USE [$db] SELECT * FROM [dbo].[$table]")
                    }
                    catch (e:SQLTimeoutException)
                    {
                        rs = null
                    }
                    catch (e:SQLException)
                    {
                        rs = null
                    }
                    catch (e: KotlinNullPointerException)
                    {
                        rs = null
                    }
                    if(rs == null)
                        return@execute
                    val columnCount = rs.metaData.columnCount
                    val rs_meta = rs.metaData
                    while (rs.next()) {
                        var map: HashMap<String, String> = HashMap()
                        for (i in 1..(columnCount)) {
                            var colum_name: String = rs_meta.getColumnName(i)
                            map[colum_name] = rs.getString(colum_name)
                        }
                        vector.addElement(map)
                    }
                    synchronized(lock)
                    {
                        lock.notify()
                    }
                })
                try
                {
                    synchronized(lock)
                    {
                        lock.wait()
                    }
                }
                catch (e: InterruptedException){}
                return vector
            }
            catch (e: SQLException)
            {
                e.printStackTrace()
                exception = e
                return null
            }

        }
        /*
            subroutine to take parameters of an update query and template it into MSSQL acceptable query, excepts update parameters to be with quotes if neccesery
         */
        fun run_command(command:String): Boolean {
            try
            {
                connection!!.isReadOnly
            }
            catch (e:SQLException)
            {
                if(e.errorCode==0)
                    ReConnect()
            }
            catch (e:KotlinNullPointerException)
            {
                ReConnect()
            }
            if(!isvalid)
                return false
            try
            {
                if(connection!!.isReadOnly)
                {
                    return false
                }
                var return_value:Boolean = false
                var lock = java.lang.Object()
                AsyncTask.execute { Runnable {
                    try
                    {
                        return_value = connection!!.prepareStatement(command).executeUpdate()>=0
                    }
                    catch (e:SQLTimeoutException)
                    {
                    }
                    catch (e:SQLException)
                    {
                    }
                    synchronized(lock)
                    {
                        lock.notify()
                    }
                }.run() }
                try {
                    synchronized(lock)
                    {
                        lock.wait()
                    }
                }
                catch (e: InterruptedException){}
                return return_value
            }
            catch (e: SQLException)
            {
                e.printStackTrace()
                exception = e
                return false
            }

        }
        /*
            subroutine to handle disconnection
         */
        fun Disconnect()
        {
            if(this.isvalid)
            {
                this.isvalid = false
                connection!!.close()
            }
        }

        /*
            subroutine to handle result set converted vector to String, for testing purposes
         */
        fun VectorToString(vector: Vector<HashMap<String, String>>?): String {
            if(vector == null)
                return "Empty"
            var str: String = ""
            var i: Int = 0
            for (item in vector) {
                str += "row ${++i}: "
                for (colum in item) {
                    str += "[${colum.key}] = ${colum.value} "
                }
                str += "\n"

            }
            return str
        }

        /*
            subroutine in charge to check if connection is alive
         */
        public fun isAlive():Boolean
        {
            try
            {
                connection!!.isReadOnly
            }
            catch (e:SQLException)
            {
                if(e.errorCode==0)
                {
                    ReConnect()
                }
            }
            catch (e:KotlinNullPointerException)
            {
                ReConnect()
            }
            if(!isvalid)
                return false
            var lock = java.lang.Object()
            AsyncTask.execute { Runnable {
                try
                {
                    connection!!.prepareStatement(context.getString(R.string.get_date_from_server)).execute()
                }
                catch (e:SQLException)
                {
                    e.printStackTrace()
                }
                synchronized(lock)
                {
                    lock.notify()
                }
            } }

            try
            {
             synchronized(lock)
             {
                 lock.wait()
             }
            }
            catch (e:InterruptedException){}
            return true
        }

        /*
            android lifecycle - is in charge of giving a living context, maybe be called once on main activity for all application lifecycle

            IS CALLED ON MAINACTIVITY, ITS ENOUGH HANDLING ON THAT DEPARTMENT
         */
        fun refresh_context(con: Context)
        {
            this.context = con
        }

        fun ReConnect():Boolean
        {
            isvalid=false
            connection=null
            return Connect(context, username, password)

        }

        fun construct_add_str(db: String, table: String, vector: Vector<String>, map: HashMap<String, String>):String
        {
            var command: String = "USE [$db] " +
                    "INSERT INTO [dbo].[$table] ("

            for (item in vector) {
                command += "[$item]"
                if (item != vector.lastElement())
                    command += ","
            }
            command += ") VALUES ("

            for (item in vector) {
                command += map[item]
                if (item != vector.lastElement())
                    command += ","
            }
            command += ")"
            return command
        }

        fun construct_remove_str(db: String, table: String, where_clause: String, compare_to: Array<String>, type: String):String
        {
            var command: String = "USE [$db]" +
                    " DELETE FROM [dbo].[$table] WHERE "
            for (item in compare_to)
            {
                command += "CONVERT($type,$where_clause) = $item "
                if (item != compare_to.last())
                    command += " OR "
            }
            return command
        }

        fun construct_update_str(db: String, table: String, where_clause: String, compare_to: Array<String>, type: String, update_to: HashMap<String, String>):String
        {
            var command: String = "USE [$db]" +
                    " UPDATE [dbo].[$table] SET "
            var breaker: Int = 0
            for (item in update_to) {
                command += " [${item.key}] = ${item.value} "
                breaker++
                if (breaker < update_to.size)
                    command += " , "
                else
                    break
            }
            command += " WHERE "
            for (item in compare_to) {
                command += "CONVERT($type,$where_clause) = $item "
                if (item != compare_to.last())
                    command += " OR "
            }
            return command
        }
    }
}