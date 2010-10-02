import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.*;
import java.io.*;

public class TimeKeeper extends HttpServlet implements Runnable
   {
   private ConnectionPool pool;
   private int numOfConcurrentLeagues=0;
   private TimeKeeper timeKeeper;
   private Thread timeKeeperThread;
   private static ServletContext context;
   private static String servletName="TimeKeeper";
   public static int numOfLines=200;

   public void init()
      {
      context = getServletContext();
      timeKeeper=new TimeKeeper();
      timeKeeperThread=new Thread(timeKeeper);
      timeKeeperThread.start();
      }

   public void run()
      {
	  Routines.writeToLog(servletName,"Server Started",true,context);
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        numOfConcurrentLeagues=Routines.safeParseInt(context.getInitParameter("concurrentLeagues"));
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
            Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      LeagueRunner[] leagueRunners=new LeagueRunner[numOfConcurrentLeagues];
      Thread[] threadArray=new Thread[numOfConcurrentLeagues];
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
	  try
		{
		Statement sql=database.createStatement();
		sql.executeUpdate("UPDATE leagues " +
						  "SET LockDown=1");
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to lock leagues : " + error,false,context);
		}
	  System.out.println("Starting Export");
	  FileWriter dumpFile;
	  BufferedWriter dumpFileBuffer;
	  String date=Routines.getDateTime(false);
	  String file=context.getRealPath("/");
	  try
		{
		dumpFile=new FileWriter(file + "/Data/dump.sql");
		dumpFileBuffer=new BufferedWriter(dumpFile);
		dumpFileBuffer.write("USE clubpit;");
		dumpFileBuffer.newLine();	
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SHOW TABLES FROM clubpit");
		  while(queryResult.next())
		    {
			dumpFileBuffer.write("DROP TABLE IF EXISTS "+queryResult.getString(1)+";");
			dumpFileBuffer.newLine();
			Statement sql2=database.createStatement();
			ResultSet queryResult2;
			queryResult2=sql2.executeQuery("SHOW CREATE TABLE "+queryResult.getString(1));
			while(queryResult2.next())
			  {
			  dumpFileBuffer.write(queryResult2.getString(2)+";");	
			  dumpFileBuffer.newLine();
			  }	
			queryResult2=sql2.executeQuery("SHOW FIELDS FROM "+queryResult.getString(1));
			String insertText="INSERT INTO "+queryResult.getString(1)+" (";
			boolean first=true;
			int numOfFields=0;
			while(queryResult2.next())
			  {
			  numOfFields++;	
			  if(!first)
			    {	
				insertText+=(",");	
			    }
			  else
			    {
			    first=false;	  
			    }
			  insertText+=(queryResult2.getString(1));	
			  }	
			insertText+=") VALUES (";  
			queryResult2=sql2.executeQuery("SELECT * FROM "+queryResult.getString(1));
			while(queryResult2.next())
			  {
			  first=true;		
			  dumpFileBuffer.write(insertText);
			  for(int currentItem=0;currentItem<numOfFields;currentItem++)
			     {	
				 if(!first)
				   {	
				   dumpFileBuffer.write(",");	
				   }
				 else
				   {
				   first=false;	  
				   }
			     dumpFileBuffer.write("\""+queryResult2.getString(currentItem+1)+"\"");
			     }	
			  dumpFileBuffer.write(");");  
			  dumpFileBuffer.newLine();  			     
			  }	
			}
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to retrieve dump data : " + error,false,context);
		  }
		dumpFileBuffer.newLine();
		dumpFileBuffer.close();
		}
	  catch(IOException error)
		{
		Routines.writeToLog(servletName,"Unable to create dump file : " + error,false,context);	
		}
	  System.out.println("Export Ended");	        
      try
        {
        Statement sql=database.createStatement();
        sql.executeUpdate("UPDATE leagues " +
                          "SET LockDown=0");
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to unlock leagues : " + error,false,context);
        }
      pool.returnConnection(database);
      boolean loop=true;
      while(loop)
           {
           pool.debug(context);	
           for(int currentLeague=0;currentLeague<leagueRunners.length;currentLeague++)
              {
              if(leagueRunners[currentLeague]!=null)
                {
                if(!leagueRunners[currentLeague].stillActive())
                  {
                  leagueRunners[currentLeague]=null;
                  threadArray[currentLeague]=null;
                  }
                }
              }
           Calendar calendar=Calendar.getInstance();
           database=null;
           try
             {
             database=pool.getConnection(servletName);
             }
           catch(SQLException error)
             {
             Routines.writeToLog(servletName,"Unable to connect to database 2 : " + error,false,context);
             }
           try
             {
             Statement sql=database.createStatement();
             ResultSet queryResult;
             queryResult=sql.executeQuery("SELECT LeagueNumber,DateTimeStamp,Alpha " +
                                          "FROM leagues " +
                                          "WHERE LockDown=0");
             while(queryResult.next())
                  {
                  int leagueNumber=queryResult.getInt(1);
                  String leagueDate=queryResult.getString(2);
                  int alpha=queryResult.getInt(3);
                  int turnaround=0;
                  if(alpha==1)
                    {
                    turnaround=Routines.safeParseInt(context.getInitParameter("alphadays"));
                    }
                  else
                    {
                    turnaround=Routines.safeParseInt(context.getInitParameter("standarddays"));
                    }
                  calendar.setTime(new java.util.Date());
                  calendar.add(Calendar.DATE,(turnaround*-1));
                  DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd HH:00:00");
                  String currentDate=dbFormat.format(calendar.getTime());
                  String currentCCYY=currentDate.substring(0,4);
                  String currentMM=currentDate.substring(5,7);
                  String currentDD=currentDate.substring(8,10);
                  String currentHH=currentDate.substring(11,13);
                  int currentCalcDate=Routines.safeParseInt(currentCCYY+currentMM+currentDD+currentHH);
                  String leagueCCYY=leagueDate.substring(0,4);
                  String leagueMM=leagueDate.substring(5,7);
                  String leagueDD=leagueDate.substring(8,10);
                  String leagueHH=leagueDate.substring(11,13);
                  int leagueCalcDate=Routines.safeParseInt(leagueCCYY+leagueMM+leagueDD+leagueHH);
                  if(currentCalcDate>=leagueCalcDate)
                    {
                    boolean slotFound=false;
                    for(int currentLeague=0;currentLeague<leagueRunners.length&&!slotFound;currentLeague++)
                       {
                       if(leagueRunners[currentLeague]==null)
                         {
                         leagueRunners[currentLeague]=new LeagueRunner(context,leagueNumber);
                         threadArray[currentLeague]=new Thread(leagueRunners[currentLeague]);
                         threadArray[currentLeague].start();
                         slotFound=true;
                         }
                       }
                    if(!slotFound)
                      {
                      Routines.writeToLog(servletName,"Increase parameter concurrentLeagues",false,context);
                      }
                    }
                  }
             }
           catch(SQLException error)
             {
             Routines.writeToLog(servletName,"Unable to find leagues : " + error,false,context);
             }
           pool.returnConnection(database);
           database=null;
           calendar=Calendar.getInstance();
           calendar.setTime(new java.util.Date());
           int minutes=calendar.get(Calendar.MINUTE);
           int seconds=calendar.get(Calendar.SECOND);
           int sleepSeconds=((59-minutes)*60)+(60-seconds);
           try
             {
             Thread.sleep(sleepSeconds*1000);
             }
           catch(InterruptedException error)
             {
             Routines.writeToLog(servletName,"InterruptError : " + error,false,context);
             }
           }
      }
   }