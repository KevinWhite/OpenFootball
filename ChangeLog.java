import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.*;

public class ChangeLog extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="ChangeLog";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
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
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
	  PrintWriter webPageOutput=null;
	  try
		{
		webPageOutput=response.getWriter();
		}
	  catch(IOException error)
		{
		Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
		}
      HttpSession session=request.getSession();
	  String action=request.getParameter("action");
	  if("New Entry".equals(action)||
		 "Change Entry".equals(action))
		 {
		 }
	  else
		{
		session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
		}
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead("Change Log",//title
                             true,//showMenu
                             0,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//Schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
     webPageOutput.println("<CENTER>");
     webPageOutput.println("<IMG SRC=\"../Images/Log.gif\"" +
                           " WIDTH='57' HEIGHT='100' ALT='Version Log'>");
     webPageOutput.println("</CENTER>");
     int changeLog=0;
	 if("Change Entry".equals(action))
	   {
	   boolean changeRequested=false;
	   int changeCount=0;
	   try
		 {
		 Statement sql=database.createStatement();
		 ResultSet queryResult;
		 queryResult=sql.executeQuery("SELECT ChangeLog " +
									  "FROM changelog ");
		 while(queryResult.next())
			  {
			  changeLog=queryResult.getInt(1);
			  if("true".equals(request.getParameter(String.valueOf(changeLog))))
				{
				changeCount++;
				if(!changeRequested)
				  {
				  changeRequested=true;
				  }
				}
			  }
		 }
	   catch(SQLException error)
		 {
		 Routines.writeToLog(servletName,"Unable to find changelog entries : " + error,false,context);	
		 }
	   if(!changeRequested)
		 {
		 session.setAttribute("message","No entry selected");
		 action="";
		 }
	   if(changeCount>1)
		 {
		 session.setAttribute("message","Please select only one entry to change");
		 action="";
		 }
	   }
	 boolean updated=false;  
	 if("Store New Entry".equals(action)||
		"Store Changed Entry".equals(action)||
		"Delete Entry".equals(action))
		{
		updated=updateEntry(action,
		 				    session,
							request,
							database);
		}	   
	webPageOutput.println("<FORM ACTION=\"http://" +
					      request.getServerName() +
					      ":" +
					      request.getServerPort() +
					      request.getContextPath() +
					      "/servlet/ChangeLog\" METHOD=\"GET\">");
	 if((String)session.getAttribute("message")!=null)
	   {
	   Routines.tableStart(false,webPageOutput);
	   Routines.tableHeader("Messages",0,webPageOutput);
	   Routines.tableDataStart(true,false,true,true,false,0,0,"scoresrow",webPageOutput);
	   Routines.messageCheck(false,request,webPageOutput);
	   Routines.tableDataEnd(true,false,true,webPageOutput);
	   Routines.tableEnd(webPageOutput);
	   webPageOutput.println(Routines.spaceLines(1));
	   }
	if("New Entry".equals(action)||
	   "Change Entry".equals(action))
		{
		formScreen(action,
				   database,
				   session,
				   request,
				   webPageOutput);
		}
	else
		{
		viewScreen(action,
				   updated,
				   session,
				   database,
				   request,
				   response,
				   webPageOutput);
		}
		pool.returnConnection(database);
		Routines.WriteHTMLTail(request,response,webPageOutput);
		}

 private void viewScreen(String action,
						 boolean updated,
						 HttpSession session,
						 Connection database,
						 HttpServletRequest request,
						 HttpServletResponse response,
						 PrintWriter webPageOutput)
	{		 
     Routines.tableStart(false,webPageOutput);
     int majorVersion=0;
     int minorVersion=0;
     if("View".equals((String)session.getAttribute("action")))
       {
       String version=(String)session.getAttribute("version");
       int dot=version.indexOf(".");
       majorVersion=Routines.safeParseInt(version.substring(0,dot));
       minorVersion=Routines.safeParseInt(version.substring(dot+1,version.length()));
       }
     else
       {
       majorVersion=Routines.safeParseInt((String)session.getAttribute("majorVersion"));
       minorVersion=Routines.safeParseInt((String)session.getAttribute("minorVersion"));
       }
     try
       {
       Statement sql=database.createStatement();
       ResultSet queryResults=sql.executeQuery("SELECT MajorVersion,MinorVersion,ChangeDate " +
                                               "FROM changelog " +
                                               "ORDER BY MajorVersion DESC,MinorVersion DESC, ChangeLog DESC");
       int currentMajorVersion=0;
       int currentMinorVersion=0;
	   java.util.Date currentDate=null;
       String selected="";
       String minorVersionText="";
       String selectText="<SELECT NAME=\"version\">";
       while(queryResults.next())
         {
         int tempMajorVersion=queryResults.getInt(1);
         int tempMinorVersion=queryResults.getInt(2);
         currentDate=queryResults.getDate(3);
         if(tempMajorVersion!=currentMajorVersion||
            tempMinorVersion!=currentMinorVersion)
            {
            currentMajorVersion=tempMajorVersion;
            currentMinorVersion=tempMinorVersion;
            if(tempMajorVersion==majorVersion&&
               tempMinorVersion==minorVersion)
               {
               selected="SELECTED ";
               }
            else
               {
               selected="";
               }
            minorVersionText=Routines.minorVersionText(tempMinorVersion);
            if(majorVersion==0&&minorVersion==0)
               {
               majorVersion=tempMajorVersion;
               minorVersion=tempMinorVersion;	
               }
            selectText=selectText +
                       " " +
                       "<OPTION " +
                       selected +
                       "VALUE=\"" +
                       tempMajorVersion +
                       "." +
                       tempMinorVersion +
                       "\">" +
                       "Version " +
                       tempMajorVersion +
                       "." +
                       minorVersionText +
                       " loaded " +
                       Routines.reformatDate(currentDate);
            }
          }
       selectText=selectText + " " + "</SELECT>";
       selectText=selectText + " " + "<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">";
       selectText=selectText + "<BR>";
	   selectText=selectText + " " + "<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"New Entry\">";
	   selectText=selectText + " " + "<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"Change Entry\">";
	   selectText=selectText + " " + "<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"Delete Entry\">";
       selectText=selectText + " " + "<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">";
       Routines.tableHeader(selectText,
                            3,
                            webPageOutput);
       queryResults=sql.executeQuery("SELECT ChangeLog,MajorVersion,MinorVersion,ChangeDate,ChangeType,ChangeText " +
                                     "FROM changelog " +
                                     "WHERE MajorVersion = " + majorVersion + " " +
                                     "AND MinorVersion = " + minorVersion + " " +
                                     "ORDER BY ChangeType,ChangeLog ASC");
       int changeLog=0;
       int changeType=0;
       int currentType=0;
       String changeText="";
       String typeText="";
	   java.util.Date changeDate=null;;
       while(queryResults.next())
	     {
         changeLog=queryResults.getInt(1);
         majorVersion=queryResults.getInt(2);
         minorVersion=queryResults.getInt(3);
         changeDate=queryResults.getDate(4);
         changeType=queryResults.getInt(5);
         changeText=queryResults.getString(6);
         minorVersionText=Routines.minorVersionText(minorVersion);
         if(changeType!=currentType)
           {
           currentType=changeType;
           String titleText="";
           if(changeType==1)
             {
             titleText="Bug Fixes";
             typeText="B";
             }
           if(changeType==2)
             {
             titleText="New Features";
             typeText="N";
             }
           Routines.tableHeader(titleText,3,webPageOutput);
           }
		 Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);  
		 webPageOutput.println("<INPUT TYPE=\"CHECKBOX\" NAME=\""+changeLog+"\" VALUE=\"true\">");  
		 Routines.tableDataEnd(false,false,false,webPageOutput);
		 Routines.tableDataStart(true,false,false,false,false,15,0,"scoresrow",webPageOutput);
         webPageOutput.println(majorVersion + "." + minorVersionText + "-" + changeLog + "-" + typeText);
         Routines.tableDataEnd(false,false,false,webPageOutput);
         Routines.tableDataStart(true,false,false,false,false,80,0,"scoresrow",webPageOutput);
         webPageOutput.println(changeText);
         Routines.tableDataEnd(false,false,true,webPageOutput);
         }
       }
     catch(SQLException error)
       {
	   Routines.writeToLog(servletName,"Unable to access ChangeLog : " + error,false,context);	
       }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("</FORM>");
      pool.returnConnection(database);
      webPageOutput.println(Routines.spaceLines(15));
      }

	private void formScreen(String action,
							Connection database,
							HttpSession session,
							HttpServletRequest request,
							PrintWriter webPageOutput)
	 {
	 int changeLog=0;
	 int bugNew=0;
	 String changeText="";
	 Routines.tableStart(false,webPageOutput);
	 boolean newEntry=false;
	 if("Change Entry".equals(action))
	   {
	   try
		 {
		 Statement sql=database.createStatement();
		 ResultSet queryResult;
		 queryResult=sql.executeQuery("SELECT ChangeLog " +
									  "FROM changelog " +
									  "ORDER BY ChangeLog DESC");
		 int tempChangeLog=0;
		 while(queryResult.next())
			  {
			  tempChangeLog=queryResult.getInt(1);
			  if("true".equals(request.getParameter(String.valueOf(tempChangeLog))))
				{
				queryResult=sql.executeQuery("SELECT ChangeLog,ChangeText,ChangeType " +
											 "FROM changelog " +
											 "WHERE ChangeLog=" + tempChangeLog);
				if(queryResult.first())
				  {
				  changeLog=queryResult.getInt(1);
				  changeText=queryResult.getString(2);
				  bugNew=queryResult.getInt(3);
				  }
				else
				  {
				  Routines.writeToLog(servletName,"Unable to find entry (" + tempChangeLog + ")",false,context);	
				  }
				}
			  }
		   }
	  catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Unable to retrieve changelog : " + error,false,context);	
		   }
	 Routines.tableHeader("Amend details of entry",3,webPageOutput);
	 }
	 if("New Entry".equals(action))
	   {
	   Routines.tableHeader("Enter details of new entry",3,webPageOutput);
	   newEntry=true;
	   Routines.tableDataStart(true,false,false,true,false,25,0,"scoresrow",webPageOutput);
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"entryType\" VALUE=\"1\" CHECKED >Current");
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"entryType\" VALUE=\"2\">Minor");
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"entryType\" VALUE=\"3\">Major");
	   Routines.tableDataEnd(false,false,false,webPageOutput);
	   Routines.tableDataStart(true,false,false,false,false,25,0,"scoresrow",webPageOutput);
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"bugNew\" VALUE=\"1\" CHECKED >Bug");
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"bugNew\" VALUE=\"2\">New");
	   Routines.tableDataEnd(false,false,false,webPageOutput);
	   Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
	   webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"changeText\" SIZE=\"50\" MAXLENGTH=\"50\" VALUE=\"" + changeText + "\">");
	   Routines.tableDataEnd(false,false,false,webPageOutput);
	   }
	 else
	   {  
	   Routines.tableDataStart(true,false,false,true,false,25,0,"scoresrow",webPageOutput);
	   String selected="";
	   if(bugNew==1)
	     {
	     selected=" CHECKED ";	
	     }
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"bugNew\" VALUE=\"1\""+selected+">Bug");
	   selected="";
	   if(bugNew==2)
		 {
		 selected=" CHECKED ";	
		 }
	   webPageOutput.print("<INPUT TYPE=RADIO NAME=\"bugNew\" VALUE=\"2\""+selected+">New");
	   Routines.tableDataEnd(false,false,false,webPageOutput);
	   Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
	   webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"changeText\" SIZE=\"50\" MAXLENGTH=\"50\" VALUE=\"" + changeText + "\">");
	   Routines.tableDataEnd(false,false,false,webPageOutput);
	   }
	 Routines.tableEnd(webPageOutput);
	 webPageOutput.println(Routines.spaceLines(1));
	 Routines.tableStart(false,webPageOutput);
	 Routines.tableHeader("Actions",1,webPageOutput);
	 Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
	 if("New Entry".equals(action))
	   {
	   webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Entry\" NAME=\"action\">");
	   }
	 else
	   {
	   webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Entry\" NAME=\"action\">");
	   }
	 webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
	 Routines.tableDataEnd(false,false,true,webPageOutput);
	 Routines.tableEnd(webPageOutput);
	 webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
	 webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"changeLog\" VALUE=\"" + changeLog + "\">");
	 webPageOutput.println("</FORM>");
	 }      
 
	private synchronized boolean updateEntry(String action,
											 HttpSession session,
											 HttpServletRequest request,
											 Connection database)
	   {
	   boolean success=false;
	   int changeLog=Routines.safeParseInt(request.getParameter("changeLog"));
	   int entryType=Routines.safeParseInt(request.getParameter("entryType"));
	   int bugNew=Routines.safeParseInt(request.getParameter("bugNew"));
	   int majorVersion=0;
	   int minorVersion=0;
	   Calendar versionDateCal=Calendar.getInstance();
	   String changeText=request.getParameter("changeText");
	   try
		 {
		 // Get Latest ChangeLog details.
		 Statement sql=database.createStatement();
		 ResultSet queryResult;
		 queryResult=sql.executeQuery("SELECT MajorVersion,MinorVersion,ChangeDate " +
									  "FROM changelog " +
									  "ORDER BY MajorVersion,MinorVersion DESC");
		 if(queryResult.first())
		   {
		   majorVersion=queryResult.getInt(1);
		   minorVersion=queryResult.getInt(2);
		   java.util.Date versionDate=queryResult.getDate(3);
		   versionDateCal.setTime(versionDate);
		   }
		 }
	   catch(SQLException error)
		 {
		 Routines.writeToLog(servletName,"Unable to retrieve latest changelog : " + error,false,context);	
		 }
	   if("Store New Entry".equals(action))
		 {
		 try
		   {
		   int updates=0;
		   if(entryType!=0)
		     {
			 versionDateCal=Calendar.getInstance();	
		     }
		   if(entryType==2)
		     {
		     minorVersion++;
		     }
		   if(entryType==3)
		     {
		     minorVersion=0;
		     majorVersion++;	
		     }	  	
		   Statement sql=database.createStatement();
		   ResultSet queryResult;
		   DateFormat dbFormat=new SimpleDateFormat("yyyy-MM-dd");
		   String dateText=dbFormat.format(versionDateCal.getTime());
		   updates=sql.executeUpdate("INSERT INTO changelog (" +
									 "MajorVersion,MinorVersion,ChangeDate,ChangeType,ChangeText) " +
									 "VALUES (" +
									 majorVersion + "," +
									 minorVersion + ",'" +
			                         dateText + "'," +
									 bugNew + ",'" +
									 changeText + "')");
		   if(updates!=1)
			 {
			 Routines.writeToLog(servletName,"New entry not created, reason unknown",false,context);	
			 }
		   }
		 catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Unable to create entry : " + error,false,context);	
		   }
		 session.setAttribute("message","Entry stored successfully");
		 success=true;
		 }
	   if("Store Changed Entry".equals(action))
		 {
		 try
		   {
		   int updates=0;
		   Statement sql=database.createStatement();
		   ResultSet queryResult;
		   updates=sql.executeUpdate("UPDATE changeLog " +
									 "SET ChangeType=" + bugNew + "," +
									 "ChangeText='" + changeText + "' " +
									 "WHERE ChangeLog=" + changeLog);
		   if(updates!=1)
			 {
			 Routines.writeToLog(servletName,"Entry not updated, reason unknown",false,context);	
			 }
		   }
		 catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Unable to update entry : " + error,false,context);	
		   }
		 session.setAttribute("message","Entry changed successfully");
		 success=true;
		 }
	   if("Delete Entry".equals(action))
		 {
		 boolean deleteRequested=false;
		 try
		   {
		   Statement sql1=database.createStatement();
		   Statement sql2=database.createStatement();
		   ResultSet queryResult;
		   queryResult=sql1.executeQuery("SELECT ChangeLog " +
									     "FROM changelog");
		   while(queryResult.next())
				{
				changeLog=queryResult.getInt(1);
				if("true".equals(request.getParameter(String.valueOf(changeLog))))
				  {
				  int updates=0;
				  deleteRequested=true;
		          updates=sql2.executeUpdate("DELETE FROM changelog " +
									         "WHERE ChangeLog=" + changeLog);
		          if(updates!=1)
			        {
			        Routines.writeToLog(servletName,"Entry not deleted (" + changeLog + ")",false,context);	
			        }
				  }  
			  }    
		   }
		 catch(SQLException error)
		   {
		   Routines.writeToLog(servletName,"Unable to delete entry : " + error,false,context);	
		   }
	     if(deleteRequested)
		   {
		   session.setAttribute("message","Delete successfull");
		   }
		 else
		   {
		   session.setAttribute("message","No entry selected");
		   }
		 success=true;
		 }
	   return success;
    } 
 }