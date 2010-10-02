import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Colleges extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Colleges";

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
      if("New College".equals(action)||
         "Change College".equals(action))
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
      Routines.WriteHTMLHead("Colleges",//title
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
      int collegeNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change College".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT CollegeNumber " +
                                       "FROM colleges");
          while(queryResult.next())
               {
               collegeNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(collegeNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find college entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No college selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one college to change");
          disabledText="";
          action="";
          }
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Colleges\" METHOD=\"POST\">");
      if("New College".equals(action)||
         "Change College".equals(action))
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
      int collegeNumber=0;
      if ("Store New College".equals(action)||
          "Store Changed College".equals(action)||
          "Delete College".equals(action))
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
      Routines.tableHeader("Colleges",2,webPageOutput);
      boolean collegesFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT CollegeNumber,CollegeName " +
                                     "FROM colleges " +
                                     "ORDER BY CollegeName ASC");
        collegeNumber=0;
        String collegeName="";
        while(queryResult.next())
             {
             if(!collegesFound)
               {
               collegesFound=true;
               }
             collegeNumber=queryResult.getInt(1);
             collegeName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(collegeNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + collegeNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(collegeName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve colleges : " + error,false,context);	
        }
      if(!collegesFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Colleges found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New College\" NAME=\"action\">");
      if(collegesFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change College\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete College\" NAME=\"action\">");
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
      int collegeNumber=0;
      String collegeName="";
      Routines.tableStart(false,webPageOutput);
      if("Change College".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT CollegeNumber " +
                                       "FROM colleges");
          int tempCollegeNumber=0;
          while(queryResult.next())
               {
               tempCollegeNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempCollegeNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT CollegeNumber,CollegeName " +
                                              "FROM colleges " +
                                              "WHERE CollegeNumber=" + tempCollegeNumber);
                 if(queryResult.first())
                   {
                   collegeNumber=queryResult.getInt(1);
                   collegeName=queryResult.getString(2);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find college (" + tempCollegeNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve colleges : " + error,false,context);	
            }
      Routines.tableHeader("Amend details of College",2,webPageOutput);
      }
      if("New College".equals(action))
        {
        Routines.tableHeader("Enter details of new College",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"collegeName\" SIZE=\"20\" MAXLENGTH=\"20\" VALUE=\"" + collegeName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New College".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New College\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed College\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"collegeNumber\" VALUE=\"" + collegeNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int collegeNumber=Routines.safeParseInt(request.getParameter("collegeNumber"));
      int sequence=0;
      String collegeName=request.getParameter("collegeName");
      if("Store New College".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO colleges (" +
                                    "CollegeNumber,CollegeName,DateTimeStamp) " +
                                    "VALUES (" +
                                    collegeNumber + ",\"" +
                                    collegeName + "\",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New college not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create colleges : " + error,false,context);
          }
        session.setAttribute("message",collegeName + " college stored successfully");
        success=true;
        }
      if("Store Changed College".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE colleges " +
                                    "SET CollegeName='" + collegeName + "'," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE CollegeNumber=" + collegeNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"College not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update colleges : " + error,false,context);	
          }
        session.setAttribute("message",collegeName + " college changed successfully");
        success=true;
        }
      if("Delete College".equals(action))
        {
        boolean deleteRequested=false;	
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT CollegeNumber " +
                                         "FROM colleges");
          while(queryResult1.next())
               {
               collegeNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(collegeNumber))))
                 {
                 deleteRequested=true;	
                 updates=sql2.executeUpdate("DELETE FROM colleges " +
                                            "WHERE CollegeNumber=" + collegeNumber);
                 if(updates!=1)
                   {
			       Routines.writeToLog(servletName,"College not deleted (" + collegeNumber + ")",false,context);	
                   }
                 }
               }
             }  
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete colleges : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No college selected");
          }
        success=true;
        }
      return success;
      }
}