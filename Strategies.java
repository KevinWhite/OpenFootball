import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Strategies extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Strategies";

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
      int type=Routines.safeParseInt(request.getParameter("type"));
      String action=request.getParameter("action");
      String[] types={"Offense","Defense","Special Teams"};
      String disabledText="";
      if("New Strategy".equals(action)||
         "Change Strategy".equals(action))
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
      Routines.WriteHTMLHead("Strategies",//title
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
      int strategyNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Strategy".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT StrategyNumber " +
                                       "FROM strategies " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               strategyNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(strategyNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find strategy entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No strategy selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one strategy to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Strategy".equals(action)||
          "Store Changed Strategy".equals(action)||
          "Delete Strategy".equals(action)||
          "Move Strategy Up".equals(action)||
          "Move Strategy Down".equals(action))
          {
          updated=updateEntry(action,
                              type,
                              session,
                              request,
                              database);
          }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Strategies\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(!updated)
        {
        disabledText="";
        }
      webPageOutput.println("<SELECT" + disabledText + " NAME=\"type\">");
      String selected="";
      for(int currentType=0;currentType<types.length;currentType++)
         {
         if(currentType==type)
           {
           selected=" SELECTED";
           }
         else
           {
           selected="";
           }
         webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentType + "\">" + types[currentType]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT" + disabledText + " TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
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
      if("New Strategy".equals(action)||
         "Change Strategy".equals(action))
        {
        formScreen(action,
                   type,
                   database,
                   session,
                   request,
                   webPageOutput);
        }
      else
        {
        viewScreen(action,
                   type,
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
                           int type,
                           boolean updated,
                           HttpSession session,
                           Connection database,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           PrintWriter webPageOutput)
      {
      int strategyNumber=0;
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      Routines.tableHeader(titleText + " Strategies",2,webPageOutput);
      boolean strategiesFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT StrategyNumber,StrategyName,SubType " +
                                     "FROM strategies " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        strategyNumber=0;
        String strategyName="";
        int subType=0;
        while(queryResult.next())
             {
             if(!strategiesFound)
               {
               strategiesFound=true;
               }
             strategyNumber=queryResult.getInt(1);
             strategyName=queryResult.getString(2);
             subType=queryResult.getInt(3);
             if(subType==0)
               {
               strategyName+=" (PrePlay)";
               }
             if(subType==1)
               {
               strategyName+=" (Primary)";
               }
             if(subType==2)
               {
               strategyName+=" (Secondary)";
               }
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(strategyNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + strategyNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(strategyName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve strategies : " + error,false,context);		
        }
      if(!strategiesFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Strategies found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Strategy\" NAME=\"action\">");
      if(strategiesFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Strategy\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Strategy\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Strategy Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Strategy Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      }

      private void formScreen(String action,
                              int type,
                              Connection database,
                              HttpSession session,
                              HttpServletRequest request,
                              PrintWriter webPageOutput)
      {
      int strategyNumber=0;
      int subType=0;
      String strategyName="";
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      if(type==0)
        {
        titleText="Offensive";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      if("Change Strategy".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT StrategyNumber " +
                                       "FROM strategies " +
                                       "WHERE Type=" + type + " " +
                                       "ORDER BY Sequence DESC");
          int tempStrategyNumber=0;
          while(queryResult.next())
               {
               tempStrategyNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempStrategyNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT StrategyNumber,StrategyName,SubType " +
                                              "FROM strategies " +
                                              "WHERE StrategyNumber=" + tempStrategyNumber);
                 if(queryResult.first())
                   {
                   strategyNumber=queryResult.getInt(1);
                   strategyName=queryResult.getString(2);
                   subType=queryResult.getInt(3);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find strategy (" + tempStrategyNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve strategy: " + error,false,context);	
            }
      Routines.tableHeader("Amend details of " + titleText + " strategy",2,webPageOutput);
      }
      if("New Strategy".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " strategy",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"strategyName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + strategyName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Strategy Type");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"subType\">");
      if(subType==0)
        {
        webPageOutput.println(" <OPTION SELECTED VALUE=\"0\">PrePlay");
        webPageOutput.println(" <OPTION VALUE=\"1\">Primary");
        webPageOutput.println(" <OPTION VALUE=\"2\">Secondary");
        }
      if(subType==1)
        {
        webPageOutput.println(" <OPTION VALUE=\"0\">PrePlay");
        webPageOutput.println(" <OPTION SELECTED VALUE=\"1\">Primary");
        webPageOutput.println(" <OPTION VALUE=\"2\">Secondary");
        }
      if(subType==2)
        {
        webPageOutput.println(" <OPTION VALUE=\"0\">PrePlay");
        webPageOutput.println(" <OPTION VALUE=\"1\">Primary");
        webPageOutput.println(" <OPTION SELECTED VALUE=\"2\">Secondary");
        }
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Strategy".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Strategy\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Strategy\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"strategyNumber\" VALUE=\"" + strategyNumber + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"type\" VALUE=\"" + type + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            int type,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int strategyNumber=Routines.safeParseInt(request.getParameter("strategyNumber"));
      int sequence=0;
      String strategyName=request.getParameter("strategyName");
      int subType=Routines.safeParseInt(request.getParameter("subType"));
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM strategies " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
        if(strategyNumber==0)
          {
          //Get latest StrategyNumber.
          strategyNumber=1;
          queryResult=sql.executeQuery("SELECT StrategyNumber " +
                                       "FROM strategies " +
                                       "ORDER BY StrategyNumber DESC");
          if(queryResult.first())
            {
            strategyNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve strategies : " + error,false,context);		
        }
      if("Move Strategy Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT StrategyNumber " +
                                         "FROM strategies " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               strategyNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(strategyNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,StrategyName FROM strategies " +
                                                "WHERE StrategyNumber=" + strategyNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the strategy list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE strategies " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Strategy not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE strategies " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE StrategyNumber=" + strategyNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Strategy not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move strategies : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No strategies selected");
          }
        success=true;
        }
      if("Move Strategy Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT StrategyNumber " +
                                         "FROM strategies " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               strategyNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(strategyNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,StrategyName FROM strategies " +
                                                "WHERE StrategyNumber=" + strategyNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the strategy list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE strategies " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Strategy not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE strategies " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE StrategyNumber=" + strategyNumber);
                 if(updates!=1)
                   {
					Routines.writeToLog(servletName,"Strategies not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move strategies : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No strategies selected");
          }
        success=true;
        }
      if("Store New Strategy".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO strategies (" +
                                    "StrategyNumber,Type," +
                                    "Sequence,SubType,StrategyName,DateTimeStamp) " +
                                    "VALUES (" +
                                    strategyNumber + "," +
                                    type + "," +
                                    (sequence+1) + "," +
                                    subType + ",\"" +
                                    strategyName + "\",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New strategy not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create strategies : " + error,false,context);	
          }
        session.setAttribute("message",strategyName + " strategy stored successfully");
        success=true;
        }
      if("Store Changed Strategy".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE strategies " +
                                    "SET StrategyName='" + strategyName + "'," +
                                    "SubType=" + subType + ",DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE StrategyNumber=" + strategyNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Strategy not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update strategy : " + error,false,context);	
          }
        session.setAttribute("message",strategyName + " strategy changed successfully");
        success=true;
        }
      if("Delete Strategy".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT StrategyNumber " +
                                         "FROM strategies " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               strategyNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(strategyNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                "FROM defaultplaybook " +
                                                "WHERE PrimaryStrategyNumber=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber1=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber2=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber3=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber4=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber5=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber6=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber7=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber8=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber9=" + strategyNumber + " " +
                                                "OR SecondaryStrategyNumber10=" + strategyNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Strategy currently in use by defaultplaybook entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT PlayBookNumber " +
                                                  "FROM playbook " +
                                                  "WHERE PrimaryStrategyNumber=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber1=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber2=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber3=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber4=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber5=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber6=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber7=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber8=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber9=" + strategyNumber + " " +
                                                  "OR SecondaryStrategyNumber10=" + strategyNumber);
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Strategy currently in use by playbook entries");
                     return false;
                     }
                   else
                     {
                     queryResult2=sql2.executeQuery("SELECT PlayByPlayNumber " +
                                                    "FROM playbyplay " +
                                                    "WHERE PrimaryStrategyNumber=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber1=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber2=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber3=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber4=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber5=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber6=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber7=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber8=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber9=" + strategyNumber + " " +
                                                    "OR SecondaryStrategyNumber10=" + strategyNumber);
                     if(queryResult2.first())
                       {
                       session.setAttribute("message","Strategy currently in use by playbyplay entries");
                       return false;
                       }
                     else
                       {
                       updates=sql2.executeUpdate("DELETE FROM strategies " +
                                                  "WHERE StrategyNumber=" + strategyNumber);
                       if(updates!=1)
                         {
						 Routines.writeToLog(servletName,"Strategy not deleted (" + strategyNumber + ")",false,context);	
                         }
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT StrategyNumber " +
                                         "FROM strategies " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          strategyNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                strategyNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE strategies " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE StrategyNumber=" + strategyNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Strategy entry not reset (" + strategyNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete strategies : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No strategies selected");
          }
        success=true;
        }
      return success;
      }
}