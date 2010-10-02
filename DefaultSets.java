import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DefaultSets extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="DefaultSets";

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
      if("New Set".equals(action)||
         "Change Set".equals(action))
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
      Routines.WriteHTMLHead("Default Sets",//title
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
      int setNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Set".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SetNumber " +
                                       "FROM defaultsets");
          while(queryResult.next())
               {
               setNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(setNumber))))
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
          Routines.writeToLog(servletName,"Unable to find defaultset entries : " + error,false,context);
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No defaultset selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one set to change");
          disabledText="";
          action="";
          }
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/DefaultSets\" METHOD=\"POST\">");
      if("New Set".equals(action)||
         "Change Set".equals(action))
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
      String currentFormation="";
      int setNumber=0;
      if ("Store New Set".equals(action)||
          "Store Changed Set".equals(action)||
          "Delete Set".equals(action)||
          "Move Set Up".equals(action)||
          "Move Set Down".equals(action))
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
      Routines.tableHeader("Sets",2,webPageOutput);
      boolean setsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT SetNumber,SetName " +
                                     "FROM defaultsets " +
                                     "ORDER BY Sequence ASC");
        setNumber=0;
        String setName="";
        while(queryResult.next())
             {
             if(!setsFound)
               {
               setsFound=true;
               }
             setNumber=queryResult.getInt(1);
             setName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(setNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + setNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(setName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve sets : " + error,false,context);
        }
      if(!setsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Sets found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Set\" NAME=\"action\">");
      if(setsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Set\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Set\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Set Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Set Down\" NAME=\"action\">");
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
      int setNumber=0;
      String setName="";
      Routines.tableStart(false,webPageOutput);
      if("Change Set".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT SetNumber " +
                                       "FROM defaultsets " +
                                       "ORDER BY Sequence DESC");
          int tempSetNumber=0;
          while(queryResult.next())
               {
               tempSetNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempSetNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT SetNumber,SetName " +
                                              "FROM defaultsets " +
                                              "WHERE SetNumber=" + tempSetNumber);
                 if(queryResult.first())
                   {
                   setNumber=queryResult.getInt(1);
                   setName=queryResult.getString(2);
                   }
                 else
                   {
                   Routines.writeToLog(servletName,"Unable to find set (" + tempSetNumber + ")",false,context);
                   }
                 }
               }
            }
       catch(SQLException error)
            {
            Routines.writeToLog(servletName,"Unable to retrieve set: " + error,false,context);
            }
      Routines.tableHeader("Amend details of Set",2,webPageOutput);
      }
      if("New Set".equals(action))
        {
        Routines.tableHeader("Enter details of new Set",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"setName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + setName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Set".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Set\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Set\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"setNumber\" VALUE=\"" + setNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int setNumber=Routines.safeParseInt(request.getParameter("setNumber"));
      int sequence=0;
      String setName=request.getParameter("setName");
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM defaultsets " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(setNumber==0)
          {
          //Get latest SetNumber.
          setNumber=1;
          queryResult=sql.executeQuery("SELECT SetNumber " +
                                       "FROM defaultsets " +
                                       "ORDER BY SetNumber DESC");
          if(queryResult.first())
            {
            setNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to retrieve defaultsets : " + error,false,context);
        }
      if("Move Set Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SetNumber " +
                                         "FROM defaultsets " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               setNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(setNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SetName FROM defaultsets " +
                                                "WHERE SetNumber=" + setNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the sets list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsets " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Set not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsets " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE SetNumber=" + setNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Set not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultsets : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No Sets selected");
          }
        success=true;
        }
      if("Move Set Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SetNumber " +
                                         "FROM defaultsets " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               setNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(setNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,SetName FROM defaultsets " +
                                                "WHERE SetNumber=" + setNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the set list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsets " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Set not moved (prior), reason unknown",false,context);
                   }
                 updates=sql1.executeUpdate("UPDATE defaultsets " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE SetNumber=" + setNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"defaultsets not moved (current), reason unknown",false,context);
                   }
                 }
               }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to move defaultsets : " + error,false,context);
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No sets selected");
          }
        success=true;
        }
      if("Store New Set".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO defaultsets (" +
                                    "SetNumber,Sequence,SetName,DateTimeStamp) " +
                                    "VALUES (" +
                                    setNumber + "," +
                                    (sequence+1) + ",\"" +
                                    setName + "\",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"New set not created, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to create defaultsets : " + error,false,context);
          }
        session.setAttribute("message",setName + " set stored successfully");
        success=true;
        }
      if("Store Changed Set".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE defaultsets " +
                                    "SET SetName='" + setName + "'," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE SetNumber=" + setNumber);
          if(updates!=1)
            {
            Routines.writeToLog(servletName,"Set not updated, reason unknown",false,context);
            }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to update defaultsets : " + error,false,context);
          }
        session.setAttribute("message",setName + " set changed successfully");
        success=true;
        }
      if("Delete Set".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT SetNumber " +
                                         "FROM defaultsets");
          while(queryResult1.next())
               {
               setNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(setNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                "FROM defaultplaybook " +
                                                "WHERE SetNumber=" + setNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Set currently in use by defaultplaybook entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                  "FROM playbook " +
                                                  "WHERE SetNumber=" + setNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Set currently in use by playbook entries");
                     return false;
                     }
                   else
                     {
                     updates=sql2.executeUpdate("DELETE FROM defaultsets " +
                                                "WHERE SetNumber=" + setNumber);
                     if(updates!=1)
                       {
                       Routines.writeToLog(servletName,"Set not deleted (" + setNumber + ")",false,context);
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT SetNumber " +
                                         "FROM defaultsets " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          setNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                setNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE defaultsets " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE SetNumber=" + setNumber);
                 if(updates!=1)
                   {
                   Routines.writeToLog(servletName,"Set entry not reset (" + setNumber + ")",false,context);
                   }
                }
          }
        catch(SQLException error)
          {
          Routines.writeToLog(servletName,"Unable to delete defaultsets : " + error,false,context);
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No set selected");
          }
        success=true;
        }
      return success;
      }
}