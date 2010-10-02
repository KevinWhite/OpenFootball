import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class GameSettings extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="GameSettings";

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

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
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
      String disabledText="";
      if("New Setting".equals(action)||
         "Change Setting".equals(action))
         {
         disabledText=" DISABLED";
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
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead("GameSettings",//title
                             false,//showMenu
                             11,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      int settingNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Setting".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SettingNumber " +
                                       "FROM gamesettings");
          while(queryResult.next())
               {
               settingNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(settingNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find setting entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No setting selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one setting to change");
          disabledText="";
          action="";
          }
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/GameSettings\" METHOD=\"POST\">");
      if("New Setting".equals(action)||
         "Change Setting".equals(action))
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
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      boolean updated=true;
      int settingNumber=0;
      if ("Store New Setting".equals(action)||
          "Store Changed Setting".equals(action)||
          "Delete Setting".equals(action))
          {
          updated=updateEntry(action,
                              session,
                              request,
                              database);
          }
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
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("GameSettings",3,webPageOutput);
      boolean settingFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SettingNumber,SettingName,Value " +
                                     "FROM gamesettings " +
                                     "ORDER BY SettingNumber DESC");
        settingNumber=0;
        while(queryResult.next())
             {
             if(!settingFound)
               {
               settingFound=true;
               }
             settingNumber=queryResult.getInt(1);
             String settingName=queryResult.getString(2);
             int settingValue=queryResult.getInt(3);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(settingNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + settingNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
			 Routines.tableDataStart(true,false,false,false,false,50,0,"scoresrow",webPageOutput);
			 webPageOutput.println(settingName);
			 Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,45,0,"scoresrow",webPageOutput);
             webPageOutput.println(settingValue);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve settings : " + error,false,context);	
        }
      if(!settingFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No GameSettings found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Setting\" NAME=\"action\">");
      if(settingFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Setting\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Setting\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(String action,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int settingNumber=0;
      String settingName="";
      int settingValue=0;
      Routines.tableStart(false,webPageOutput);
      if("Change Setting".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SettingNumber " +
                                       "FROM gamesettings " +
                                       "ORDER BY SettingNumber DESC");
          int tempSettingNumber=0;
          while(queryResult.next())
               {
               tempSettingNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(tempSettingNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT SettingNumber,SettingName,Value " +
                                              "FROM gamesettings " +
                                              "WHERE SettingNumber=" + tempSettingNumber);
                 if(queryResult.first())
                   {
                   settingNumber=queryResult.getInt(1);
                   settingName=queryResult.getString(2);
                   settingValue=queryResult.getInt(3);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find setting (" + tempSettingNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve setting: " + error,false,context);	
            }
      Routines.tableHeader("Amend details of Setting",2,webPageOutput);
      }
      if("New Setting".equals(action))
        {
        Routines.tableHeader("Enter details of new Setting",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Setting Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,30,0,"scoresrow",webPageOutput);
	  webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"settingName\" SIZE=\"20\" MAXLENGTH=\"20\" VALUE=\"" + settingName + "\">");
	  Routines.tableDataEnd(false,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
	  webPageOutput.print("Setting Value");
	  Routines.tableDataEnd(false,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,false,false,false,40,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"settingValue\">");
	  String selected="";
	  for(int currentNumber=0;currentNumber<100;currentNumber++)
		 {
		 if(currentNumber==settingValue)
		   {
		   selected=" SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + currentNumber);
		 }
	  webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Setting".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Setting\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Setting\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"settingNumber\" VALUE=\"" + settingNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int settingNumber=Routines.safeParseInt(request.getParameter("settingNumber"));
	  String settingName=request.getParameter("settingName");
	  int settingValue=Routines.safeParseInt(request.getParameter("settingValue"));
      if(settingNumber==0)
        {
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
          //Get latest settingNumber.
          settingNumber=1;
          queryResult=sql.executeQuery("SELECT SettingNumber " +
                                       "FROM gamesettings " +
                                       "ORDER BY SettingNumber DESC");
          if(queryResult.first())
            {
            settingNumber=queryResult.getInt(1) + 1;
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to retrieve settings : " + error,false,context);	
          }
        }  
      if("Store New Setting".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO gamesettings (" +
                                    "SettingNumber,SettingName,Value,DateTimeStamp) " +
                                    "VALUES (" +
                                    settingNumber + ",\"" +
                                    settingName + "\"," +
                                    settingValue + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New setting not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create gamesettings : " + error,false,context);
          }
        session.setAttribute("message",settingName + " setting stored successfully");
        success=true;
        }
      if("Store Changed Setting".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE gamesettings " +
                                    "SET SettingName=\"" + settingName + "\"," +
                                    "Value=" + settingValue + "," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE SettingNumber=" + settingNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Setting not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update settings : " + error,false,context);	
          }
        session.setAttribute("message",settingName + " setting changed successfully");
        success=true;
        }
      if("Delete Setting".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SettingNumber " +
                                         "FROM gamesettings");
          while(queryResult1.next())
               {
               settingNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(settingNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM gamesettings " +
                                            "WHERE SettingNumber=" + settingNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"setting not deleted (" + settingNumber + ")",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete gamesettings : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No setting selected");
          }
        success=true;
        }
      return success;
      }
}