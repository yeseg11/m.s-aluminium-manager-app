package com.example.chaosruler.msa_manager.SQLITE_helpers.sync_table

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.chaosruler.msa_manager.BuildConfig
import com.example.chaosruler.msa_manager.MSSQL_helpers.remote_big_table_helper
import com.example.chaosruler.msa_manager.MSSQL_helpers.remote_inventory_table_helper
import com.example.chaosruler.msa_manager.R
import com.example.chaosruler.msa_manager.dataclass_for_SQL_representation.inventory_data
import com.example.chaosruler.msa_manager.services.global_variables_dataclass
import com.example.chaosruler.msa_manager.abstraction_classes.local_SQL_Helper
import com.example.chaosruler.msa_manager.dataclass_for_SQL_representation.big_table_data
import com.example.chaosruler.msa_manager.services.remote_SQL_Helper
import java.util.*
import kotlin.collections.HashMap


class local_inventory_table_helper(private var context: Context) : local_SQL_Helper(context,context.getString(R.string.LOCAL_SYNC_DATABASE_NAME),null,context.resources.getInteger(R.integer.LOCAL_INVENTORY_TABLE_VERSION),context.getString(R.string.LOCAL_INVENTORY_TABLE_NAME))
{
    private val ID: String = context.getString(R.string.LOCAL_INVENTORY_COLUMN_ID)
    private val NAME:String = context.getString(R.string.LOCAL_INVENTORY_COLUMN_NAME)
    private val DATAARAEID:String = context.getString(R.string.LOCAL_INVENTORY_COLUMN_DATAARAEID)
    private val USER:String = context.getString(R.string.LOCAL_INVENTORY_COLUMN_USERNAME)
    /*
    MUST BE CALLED, it reports to the database about the table schema, is used by the abstracted
    SQL class
 */
    init
    {
        var vector: Vector<String> = Vector()
        vector.add(ID)
        vector.add(NAME)
        vector.add(DATAARAEID)
        vector.add(USER)
        init_vector_of_variables(vector)

    }
    /*
           provides info for the abstracted SQL class
           on what the table schema is for creation
        */
    override fun onCreate(db: SQLiteDatabase)
    {
        var map: HashMap<String, String> = HashMap()
        map[ID] = "text primary key"
        map[NAME] = "text"
        map[USER] = "text"
        map[DATAARAEID] = "text"
        createDB(db,map)
    }


    /*
      adds all inventory, updates, inserts... whatever
   */
    fun sync_db()
    {
        if(!global_variables_dataclass.isLocal)
            return
        var server_vec = server_data_to_vector()
        for(item in server_vec)
        {
            add_inventory(item)
        }
    }

    /*
        converts DB to vector of inventory
     */
    fun get_local_DB():Vector<inventory_data>
    {
        var vector:Vector<inventory_data> = Vector()

        var all_db:Vector<HashMap<String,String>> = get_db()
        all_db
                .filter {
                    @Suppress("USELESS_ELVIS_RIGHT_IS_NULL")
                    (it[USER]?:null) != null && it[USER] == remote_SQL_Helper.getusername()
                }
                .forEach { vector.addElement(inventory_data((it[ID]?:"").trim(), (it[NAME]?:"").trim(), (it[DATAARAEID]?:"").trim(), (it[USER]?:"").trim())) }
        return vector
    }
    /*
           gets local data by projname
        */
    fun get_local_DB_by_projname(projid:String): Vector<inventory_data>
    {
        var vector: Vector<inventory_data> = Vector()

        var all_db: Vector<big_table_data> = global_variables_dataclass.DB_BIG!!.get_local_DB()
        var inventorydb : Vector<inventory_data> = global_variables_dataclass.DB_INVENTORY!!.get_local_DB()


        for(inventory in inventorydb)
        {
            for (big in all_db)
            {
                if((big.get_PROJECT_ID()?:"") == projid && (big.get_INVENTORY_ID()?:"") == (inventory.get_itemid()?:"1")  ) {
                    vector.addElement(inventory)
                    break
                }
            }
        }
        return vector
    }


    /*
           subroutine to convert server data to vector of inventory
        */
    fun server_data_to_vector():Vector<inventory_data>
    {
        var server_data: Vector<java.util.HashMap<String, String>> =
        if(BuildConfig.DEBUG)
        {
            var typemap: java.util.HashMap<String, String> = remote_inventory_table_helper.make_type_map()
            remote_SQL_Helper.select_columns_from_db_with_where(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_INVENTORY),typemap,context.getString(R.string.INVENTORY_DATAAREAID),context.getString(R.string.DATAAREAID_DEVELOP))
        }
        else
        {
            remote_SQL_Helper.get_all_table(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_INVENTORY))
        }
        var result_vector:Vector<inventory_data> = Vector()
        server_data.mapTo(result_vector) {
            inventory_data(
                    (it[remote_inventory_table_helper.ID]?: "").trim(), (it[remote_inventory_table_helper.NAME]?: "").trim(),
                    (it[remote_inventory_table_helper.DATAAREAID]?: "").trim(), (remote_SQL_Helper.getusername()).trim()
            )
        }
        return result_vector
    }

    /*
       subroutine to convert server data to vector of vendor by projname
    */
    fun server_data_to_vector_by_projname(projid: String): Vector<inventory_data>
    {

        var server_data_big: Vector<java.util.HashMap<String, String>> =
                if(BuildConfig.DEBUG)
                {
                    var typemap:HashMap<String,String> = remote_big_table_helper.make_type_map()
                    remote_SQL_Helper.select_columns_from_db_with_where(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_BIG),typemap,context.getString(R.string.TABLE_BIG_DATAAREAID),context.getString(R.string.DATAAREAID_DEVELOP))
                }
                else
                {
                    remote_SQL_Helper.get_all_table(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_BIG))
                }

        var server_data_inventory: Vector<java.util.HashMap<String, String>> =
                if(BuildConfig.DEBUG)
                {
                    var typemap:HashMap<String,String> = remote_inventory_table_helper.make_type_map()
                    remote_SQL_Helper.select_columns_from_db_with_where(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_INVENTORY),typemap,context.getString(R.string.INVENTORY_DATAAREAID),context.getString(R.string.DATAAREAID_DEVELOP))
                }
                else
                {
                    remote_SQL_Helper.get_all_table(context.getString(R.string.DATABASE_NAME), context.getString(R.string.TABLE_INVENTORY))
                }
        var result_vector: Vector<inventory_data> = Vector()
        for(inventory in server_data_inventory)
        {
            for (big in server_data_big)
            {
                if((big[remote_big_table_helper.PROJECTS_ID]?:"").trim() == projid && (big[remote_big_table_helper.INVENTORY_ID]?:"").trim() == (inventory[remote_inventory_table_helper.ID]?:"1").trim()  ) {
                    result_vector.addElement(inventory_data( (inventory[remote_inventory_table_helper.ID]?:"").trim(),
                            (inventory[remote_inventory_table_helper.NAME]?:"").trim(),(inventory[remote_inventory_table_helper.DATAAREAID]?:"").trim()
                            ,(remote_SQL_Helper.getusername().trim())))

                    break
                }
            }
        }

        return result_vector
    }
    /*
           subroutine that is in charge of getting the inventory class
           by query
        */
    fun get_inventory_by_inventory(inventory_data: inventory_data) // subroutine to get a inventory object
            : inventory_data?
    {
        var input_map = HashMap<String,String>()
        input_map[ID] = "'${inventory_data.get_itemid()}'"
        val vector = get_rows(input_map)
        if(vector.size > 0)
        {
            return inventory_data((vector.firstElement()[ID]?:"").trim(), (vector.firstElement()[NAME]?:"").trim(), (vector.firstElement()[DATAARAEID]?:"").trim(), (vector.firstElement()[USER]?:"").trim())
        }


        return null
    }


    /*
      add inventory mechanism
      if inventory is invalid, forget about it
      if inventory is valid, and it exists, update it
      if its a new inventory, add a new inventory to table
   */
    fun add_inventory(inventory_data: inventory_data) // subroutine that manages the inventory adding operation to the database
            : Boolean
    {
        /*
        var map:HashMap<String,String> = HashMap()
        map[ID] = inventory_data.get_itemid() ?: ""
        map[NAME] = inventory_data.get_itemname() ?: ""
        map[DATAARAEID] = inventory_data.get_DATAREAID() ?: ""
        map[USER] = inventory_data.get_USERNAME() ?: ""
        return replace(map)
        */
        return if (check_inventory( inventory_data)) // checks if inventory exists in database
            update_inventory(inventory_data,inventory_data.copy()) // if it does, lets update
        else // if it doesn't lets create a new entry for the inventory
            insert_inventory(inventory_data)
    }

    /*
          checks if inventory exists, query is not that smart, gets an ENTIRE table and than checks
          if the user is there

          // on update
          will select USERNAME only
       */
    fun check_inventory(inventory_data: inventory_data) // subroutine to check if inventory exists on the database
            : Boolean
    {
        val inventory:inventory_data? = get_inventory_by_inventory( inventory_data)
        return inventory != null
    }
    /*
        subroutine in charge of feeding schema and database information to SQL
        abstract implentation on insert queries
     */
    private fun insert_inventory(inventory_data: inventory_data):Boolean // subroutine to insert a inventory to the database
    {

        var everything_to_add:Vector<HashMap<String,String>> = Vector()

        var data: HashMap<String,String> = HashMap()
        data[ID] = (inventory_data.get_itemid() ?: "").trim()
        data[NAME] = (inventory_data.get_itemname() ?: "").trim()
        data[DATAARAEID] = (inventory_data.get_DATAREAID() ?: "").trim()
        data[USER] = (inventory_data.get_USERNAME() ?: "").trim()
        everything_to_add.addElement(data)
        return add_data(everything_to_add)
    }

    /*
      subroutine in charge of feeding information and database information to
      SQL abstraction on update queries
   */
    fun update_inventory(from:inventory_data,to:inventory_data) // subroutine to update data of a inventory that exists on the database
            : Boolean {

        var change_to:HashMap<String,String> = HashMap()
        change_to[NAME] = (to.get_itemname() ?: "").trim()
        change_to[DATAARAEID] = (to.get_DATAREAID() ?: "").trim()
        change_to[USER] = (to.get_USERNAME() ?: "").trim()
        return update_data(ID, arrayOf(from.get_itemid()?:""),change_to)
    }

    /*
        subroutine in charge of feeding information and database information to
        SQL abstraction on delete queries
     */
    fun delete_inventory( inventory_data: inventory_data):Boolean // subroutine to delete a inventory from the database (local)
    {
        if ( get_inventory_by_inventory(inventory_data)==null )
            return false
        return remove_from_db(ID, arrayOf((inventory_data.get_itemid()?:"").trim()))
    }

    /*
        get inventory by ID
     */
    public fun get_inventory_by_id(id: String):inventory_data?
    {
        var mock_obj:inventory_data = inventory_data(id,null,null,remote_SQL_Helper.getusername())
        return get_inventory_by_inventory(mock_obj)
    }
}