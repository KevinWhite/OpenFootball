import java.sql.*;
import java.util.*;
import javax.servlet.*;

public class ConnectionPool
   {
   private Hashtable connections=new Hashtable();
   private Properties properties;
   private static String servletName="ConnectionPool";

   public ConnectionPool(String driverClassName,
                         String dbURL,
                         String user,
                         String password)
                         throws SQLException,ClassNotFoundException
      {
      int initialConnections=5;
      properties=new Properties();
      properties.put("connection.driver",driverClassName);
      properties.put("connection.url",dbURL);
      properties.put("user",user);
      properties.put("password",password);
      initializePool(properties,initialConnections);
      }

   public Connection getConnection(String servletName) throws SQLException
      {
      Connection connection=null;
      Enumeration eConnections=connections.keys();
      synchronized(connections)
         {
         while(eConnections.hasMoreElements())
            {
            connection=(Connection)eConnections.nextElement();
            poolObject currentConnectionDetails=(poolObject)connections.get(connection);
            if(!currentConnectionDetails.getInUse())
              {
              try
                {
                connection.setAutoCommit(true);
                }
              catch(SQLException error)
                {
                connections.remove(connection);
                connection=getNewConnection();
                }
              currentConnectionDetails.setInUse(true);
              currentConnectionDetails.setInUseBy(servletName);  
              currentConnectionDetails.setDateTime(Routines.getDateTime(false));
              connections.put(connection,currentConnectionDetails);
              return connection;
              }
            }
         connection=getNewConnection();
         poolObject currentConnection=new poolObject(true,servletName,Routines.getDateTime(false));
         connections.put(connection,currentConnection);
         return connection;
         }
      }

   public void returnConnection(Connection returned)
      {
      if(connections.containsKey(returned))
        {
		poolObject returnedConnectionDetails=(poolObject)connections.get(returned);	
		returnedConnectionDetails.setInUse(false);
		returnedConnectionDetails.setInUseBy("");
		returnedConnectionDetails.setDateTime("");
        connections.put(returned,returnedConnectionDetails);
        }
      }

   private void initializePool(Properties props,
                               int initialConnections)
           throws SQLException,ClassNotFoundException
      {
      Class.forName(props.getProperty("connection.driver"));
      for(int currentConnection=0;currentConnection<initialConnections;currentConnection++)
         {
         Connection connection=getNewConnection();
         poolObject currentConnectionDetails=new poolObject(false,"","");
         connections.put(connection,currentConnectionDetails);
         }
      }

   private Connection getNewConnection() throws SQLException
      {
      return DriverManager.getConnection(properties.getProperty("connection.url"),properties);
      }

   public void debug(ServletContext context)
      {
      Connection connection=null;
      Enumeration eConnections=connections.keys();
      Routines.writeToLog(servletName,"Number of Connections : " + connections.size(),false,context);
      int currentConnection=0;
      synchronized(connections)
         {
         while(eConnections.hasMoreElements())
            {
            currentConnection++;
            connection=(Connection)eConnections.nextElement();
			poolObject currentConnectionDetails=(poolObject)connections.get(connection);
            boolean connectionActive=currentConnectionDetails.getInUse();
            if(!connectionActive)
              {	
              Routines.writeToLog(servletName,"Connection " + currentConnection + " : Ready.",false,context);
              }
            else
              {
              Routines.writeToLog(servletName,"Connection " + currentConnection + " : In use [" + currentConnectionDetails.getInUseBy() + "@" + currentConnectionDetails.getDateTime()+"]",false,context);
              }
            }
         }
      }
   }   
  
class poolObject 
   {
   private boolean inUse=false;
   private String inUseBy="";
   private String dateTime="";
   
   public poolObject(boolean inUse,
					 String inUseBy,
					 String dateTime)
	  {
	  setInUse(inUse);
	  setInUseBy(inUseBy);	
	  setDateTime(dateTime); 
	  }	   
   
   public void setInUse(boolean inUse)
	 {
	 this.inUse=inUse;
	 }   	
   public void setInUseBy(String inUseBy)
	 {
	 this.inUseBy=inUseBy;			 
	 }
   public void setDateTime(String dateTime)
     {
     this.dateTime=dateTime;		 
     }
   public boolean getInUse()
	 {
	 return inUse;	
	 }
   public String getInUseBy()
	 {
	 return inUseBy;	  
	 }
   public String getDateTime()
     {
     return dateTime;		 
     }
   }	 