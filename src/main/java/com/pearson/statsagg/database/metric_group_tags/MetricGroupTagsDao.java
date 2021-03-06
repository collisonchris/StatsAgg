package com.pearson.statsagg.database.metric_group_tags;

import com.pearson.statsagg.database.DatabaseInterface;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.pearson.statsagg.database.DatabaseObjectDao;
import com.pearson.statsagg.globals.DatabaseConfiguration;
import com.pearson.statsagg.utilities.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
public class MetricGroupTagsDao extends DatabaseObjectDao<MetricGroupTag> {

    private static final Logger logger = LoggerFactory.getLogger(MetricGroupTagsDao.class.getName());
   
    private final String tableName_ = "METRIC_GROUP_TAGS";
    
    public MetricGroupTagsDao(){}
            
    public MetricGroupTagsDao(boolean closeConnectionAfterOperation) {
        databaseInterface_.setCloseConnectionAfterOperation(closeConnectionAfterOperation);
    }
    
    public MetricGroupTagsDao(DatabaseInterface databaseInterface) {
        super(databaseInterface);
    }
    
    public boolean dropTable() {
        return dropTable(MetricGroupTagsSql.DropTable_MetricGroupTags);
    }
    
    public boolean createTable() {
        List<String> databaseCreationSqlStatements = new ArrayList<>();
        
        if (DatabaseConfiguration.getType() == DatabaseConfiguration.MYSQL) {
            databaseCreationSqlStatements.add(MetricGroupTagsSql.CreateTable_MetricGroupTags_MySQL);
        }
        else {
            databaseCreationSqlStatements.add(MetricGroupTagsSql.CreateTable_MetricGroupTags_Derby);
            databaseCreationSqlStatements.add(MetricGroupTagsSql.CreateIndex_MetricGroupTags_PrimaryKey);
        }
        
        databaseCreationSqlStatements.add(MetricGroupTagsSql.CreateIndex_MetricGroupTags_ForeignKey_MetricGroupId);

        return createTable(databaseCreationSqlStatements);
    }
    
    @Override
    public MetricGroupTag getDatabaseObject(MetricGroupTag metricGroupTag) {
        if (metricGroupTag == null) return null;
        
        return getDatabaseObject(MetricGroupTagsSql.Select_MetricGroupTag_ByPrimaryKey, 
                metricGroupTag.getId()); 
    }
        
    @Override
    public boolean insert(MetricGroupTag metricGroupTag) {
        if (metricGroupTag == null) return false;
        
        return insert(MetricGroupTagsSql.Insert_MetricGroupTag, 
                metricGroupTag.getMgId(), metricGroupTag.getTag());
    }
    
    @Override
    public boolean update(MetricGroupTag metricGroupTag) {
        if (metricGroupTag == null) return false;
        
        return update(MetricGroupTagsSql.Update_MetricGroupTag_ByPrimaryKey, 
                metricGroupTag.getMgId(), metricGroupTag.getTag(), metricGroupTag.getId());
    }

    @Override
    public boolean delete(MetricGroupTag metricGroupTag) {
        if (metricGroupTag == null) return false;

        return delete(MetricGroupTagsSql.Delete_MetricGroupTag_ByPrimaryKey, 
                metricGroupTag.getId()); 
    }
    
    @Override
    public MetricGroupTag processSingleResultAllColumns(ResultSet resultSet) {
        
        try {     
            if ((resultSet == null) || resultSet.isClosed()) {
                return null;
            }

            Integer id = resultSet.getInt("ID");
            if (resultSet.wasNull()) id = null;
            
            Integer mgId = resultSet.getInt("METRIC_GROUP_ID");
            if (resultSet.wasNull()) mgId = null;
            
            String tag = resultSet.getString("TAG");
            if (resultSet.wasNull()) tag = null;
            
            MetricGroupTag metricGroupTag = new MetricGroupTag(id, mgId, tag);
            
            return metricGroupTag;
        }
        catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            return null;
        }
    }
    
    @Override
    public String getTableName() {
        return tableName_;
    }
    
    public MetricGroupTag getMetricGroupTag(int id) {
        return getDatabaseObject(MetricGroupTagsSql.Select_MetricGroupTag_ByPrimaryKey, 
                id); 
    }  
    
    public Map<Integer,List<MetricGroupTag>> getAllMetricGroupTagsByMetricGroupId() {
        List<MetricGroupTag> metricGroupTags = getAllDatabaseObjectsInTable();
        
        if (metricGroupTags == null || metricGroupTags.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<Integer,List<MetricGroupTag>> databaseObjectsInTableByMetricGroupId = new HashMap<>();

        for (MetricGroupTag metricGroupTag : metricGroupTags) {
            Integer metricGroupId = metricGroupTag.getMgId();
            
            if (databaseObjectsInTableByMetricGroupId.containsKey(metricGroupId)) {
                List<MetricGroupTag> databaseObjects = databaseObjectsInTableByMetricGroupId.get(metricGroupId);
                databaseObjects.add(metricGroupTag);
            }
            else {
                List<MetricGroupTag> databaseObjects = new ArrayList<>();
                databaseObjects.add(metricGroupTag);
                databaseObjectsInTableByMetricGroupId.put(metricGroupId, databaseObjects);
            }
        }
        
        return databaseObjectsInTableByMetricGroupId;
    }
    
    public List<MetricGroupTag> getMetricGroupTagsByMetricGroupId(int metricGroupId) {
        
        try {

            if (!isConnectionValid()) {
                return null;
            }

            databaseInterface_.createPreparedStatement(MetricGroupTagsSql.Select_MetricGroupTags_ByMetricGroupId, 100);
            databaseInterface_.addPreparedStatementParameters(metricGroupId);
            databaseInterface_.executePreparedStatement();
            
            if (!databaseInterface_.isResultSetValid()) {
                return null;
            }

            List<MetricGroupTag> metricGroupTags = new ArrayList<>();
            
            ResultSet resultSet = databaseInterface_.getResults();

            while (resultSet.next()) {
                MetricGroupTag databaseObject = processSingleResultAllColumns(resultSet);
                metricGroupTags.add(databaseObject);
            }

            return metricGroupTags;
        }
        catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            return null;
        }
        finally {
            databaseInterface_.cleanupAutomatic();
        } 
        
    }
    
    public boolean deleteByMetricGroupId(int id) {
        return delete(MetricGroupTagsSql.Delete_MetricGroupTag_ByMetricGroupId, 
                id); 
    }
    
}
