import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Positions extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Positions";

   public void init() throws ServletException
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
      String[] types={"Offense","Defense","Special Teams","Staff"};
      String disabledText="";
      if("New Position".equals(action)||
         "Change Position".equals(action))
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
      Routines.WriteHTMLHead("Positions",//title
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
      int positionNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Position".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "WHERE Type=" + type);
          while(queryResult.next())
               {
               positionNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(positionNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find position entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No position selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one position to change");
          disabledText="";
          action="";
          }
        }
      boolean updated=true;
      if ("Store New Position".equals(action)||
          "Store Changed Position".equals(action)||
          "Delete Position".equals(action)||
          "Move Position Up".equals(action)||
          "Move Position Down".equals(action))
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
                             "/servlet/Positions\" METHOD=\"POST\">");
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
      if("New Position".equals(action)||
         "Change Position".equals(action))
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
      int positionNumber=0;
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
      if(type==3)
        {
        titleText="Staff";
        }
      Routines.tableHeader(titleText + " Positions",2,webPageOutput);
      boolean positionsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionNumber,PositionName " +
                                     "FROM positions " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence ASC");
        positionNumber=0;
        String positionName="";
        while(queryResult.next())
             {
             if(!positionsFound)
               {
               positionsFound=true;
               }
             positionNumber=queryResult.getInt(1);
             positionName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(positionNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + positionNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,95,0,"scoresrow",webPageOutput);
             webPageOutput.println(positionName);
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve positions : " + error,false,context);		
        }
      if(!positionsFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Positions found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Position\" NAME=\"action\">");
      if(positionsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Position\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Position\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Down\" NAME=\"action\">");
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
      int positionNumber=0;
      int subType=0;
      int realPosition=0;
      int chartOnly=0;
      int maxRookieSalary=0;
      int maxFreeAgentSalary=0;
      int shirtMin1=0;
      int shirtMax1=0;
      int shirtMin2=0;
      int shirtMax2=0;
      String positionName="";
      String positionCode="";
      String positionDescription="";
      Routines.tableStart(false,webPageOutput);
      String titleText="";
      String[] subTypes=new String[10];
      if(type==0)
        {
        titleText="Offensive";
        subTypes[0]="Normal";
        subTypes[1]="Runner";
        subTypes[2]="Receiver";
        subTypes[3]="Runner & Receiver";
        }
      if(type==1)
        {
        titleText="Defensive";
        }
      if(type==2)
        {
        titleText="Special Teams";
        }
      if(type==3)
        {
        titleText="Staff";
        }
      if("Change Position".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "WHERE Type=" + type + " " +
                                       "ORDER BY Sequence DESC");
          int tempPositionNumber=0;
          while(queryResult.next())
               {
               tempPositionNumber=queryResult.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(tempPositionNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT PositionNumber,PositionCode,PositionName,SubType,PositionDescription,RealPosition,ChartOnly,MaxRookieSalary,MaxFreeAgentSalary,ShirtMin1,ShirtMax1,ShirtMin2,ShirtMax2 " +
                                              "FROM positions " +
                                              "WHERE PositionNumber=" + tempPositionNumber);
                 if(queryResult.first())
                   {
                   positionNumber=queryResult.getInt(1);
                   positionCode=queryResult.getString(2);
                   positionName=queryResult.getString(3);
                   subType=queryResult.getInt(4);
                   positionDescription=queryResult.getString(5);
                   realPosition=queryResult.getInt(6);
                   chartOnly=queryResult.getInt(7);
                   maxRookieSalary=queryResult.getInt(8);
                   maxFreeAgentSalary=queryResult.getInt(9);
                   shirtMin1=queryResult.getInt(10);
                   shirtMax1=queryResult.getInt(11);
                   shirtMin2=queryResult.getInt(12);
                   shirtMax2=queryResult.getInt(13);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find position (" + tempPositionNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve positions : " + error,false,context);	
            }
      Routines.tableHeader("Amend details of " + titleText + " position",2,webPageOutput);
      }
      if("New Position".equals(action))
        {
        Routines.tableHeader("Enter details of new " + titleText + " position",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Name");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"positionName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + positionName + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Position Code");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"positionCode\" SIZE=\"4\" MAXLENGTH=\"4\" VALUE=\"" + positionCode + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Maximum Rookie Salary");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"maxRookieSalary\" SIZE=\"8\" MAXLENGTH=\"8\" VALUE=\"" + maxRookieSalary + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Maximum Free Agent Salary");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"maxFreeAgentSalary\" SIZE=\"8\" MAXLENGTH=\"8\" VALUE=\"" + maxFreeAgentSalary + "\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      if(type==3)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Position Description");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<TEXTAREA ROWS=\"5\" cols=\"50\" NAME=\"positionDescription\">");
        if(positionDescription!=null&&!"".equals(positionDescription))
          {
          webPageOutput.println(positionDescription);
          }
        webPageOutput.println("</TEXTAREA>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }

      if(type==0)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Position Type");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"subType\">");
        String selectText="";
        for(int currentType=0;currentType<subTypes.length;currentType++)
           {
           if(subTypes[currentType]==null)
             {
             }
           else
             {
             if(subType==currentType)
               {
               selectText=" SELECTED";
               }
             else
               {
               selectText="";
               }
             webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentType + "\">" + subTypes[currentType]);
             }
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Real Position");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"realPosition\" VALUE=\"true\"");
      if(realPosition==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("Chart Only");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
      webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"chartOnly\" VALUE=\"true\"");
      if(chartOnly==1)
        {
        webPageOutput.print(" CHECKED");
        }
      webPageOutput.println(">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      if(type!=3)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Min Shirt Value (1)");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"minShirt1\">");
        String selectText="";
        for(int currentShirt=1;currentShirt<100;currentShirt++)
           {
           if(currentShirt==shirtMin1)
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentShirt + "\">" + currentShirt);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      if(type!=3)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Max Shirt Value (1)");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"maxShirt1\">");
        String selectText="";
        for(int currentShirt=1;currentShirt<100;currentShirt++)
           {
           if(currentShirt==shirtMax1)
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentShirt + "\">" + currentShirt);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      if(type!=3)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Min Shirt Value (2)");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"minShirt2\">");
        String selectText="";
        for(int currentShirt=1;currentShirt<100;currentShirt++)
           {
           if(currentShirt==shirtMin2)
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentShirt + "\">" + currentShirt);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      if(type!=3)
        {
        Routines.tableDataStart(true,false,false,true,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.print("Max Shirt Value (2)");
        Routines.tableDataEnd(false,false,false,webPageOutput);
        Routines.tableDataStart(true,false,false,false,false,10,0,"scoresrow",webPageOutput);
        webPageOutput.println("<SELECT NAME=\"maxShirt2\">");
        String selectText="";
        for(int currentShirt=1;currentShirt<100;currentShirt++)
           {
           if(currentShirt==shirtMax2)
             {
             selectText=" SELECTED";
             }
           else
             {
             selectText="";
             }
           webPageOutput.println(" <OPTION" + selectText + " VALUE=\"" + currentShirt + "\">" + currentShirt);
           }
        webPageOutput.println("</SELECT>");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Position".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Position\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Position\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"positionNumber\" VALUE=\"" + positionNumber + "\">");
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
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int subType=Routines.safeParseInt(request.getParameter("subType"));
      int maxRookieSalary=Routines.safeParseInt(request.getParameter("maxRookieSalary"));
      int maxFreeAgentSalary=Routines.safeParseInt(request.getParameter("maxFreeAgentSalary"));
      int shirtMin1=Routines.safeParseInt(request.getParameter("minShirt1"));
      int shirtMax1=Routines.safeParseInt(request.getParameter("maxShirt1"));
      int shirtMin2=Routines.safeParseInt(request.getParameter("minShirt2"));
      int shirtMax2=Routines.safeParseInt(request.getParameter("maxShirt2"));
      int sequence=0;
      int realPosition=0;
      int chartOnly=0;
      String positionName=request.getParameter("positionName");
      String positionCode=request.getParameter("positionCode");
      String positionDescription=request.getParameter("positionDescription");
      String tempRealPosition=request.getParameter("realPosition");
      String tempChartOnly=request.getParameter("chartOnly");
      if("true".equals(tempRealPosition))
        {
        realPosition=1;
        }
      if("true".equals(tempChartOnly))
        {
        chartOnly=1;
        }
      if(positionDescription==null)
        {
        positionDescription="";
        }
      try
        {
        // Get Latest SequenceNumber.
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT Sequence " +
                                     "FROM positions " +
                                     "WHERE Type=" + type + " " +
                                     "ORDER BY Sequence DESC");
        if(queryResult.first())
          {
          sequence=queryResult.getInt(1);
          }
		if("Store New Position".equals(action))
          {
          //Get latest PositionNumber.
          positionNumber=1;
          queryResult=sql.executeQuery("SELECT PositionNumber " +
                                       "FROM positions " +
                                       "ORDER BY PositionNumber DESC");
          if(queryResult.first())
            {
            positionNumber=queryResult.getInt(1) + 1;
            }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve positions : " + error,false,context);	
        }
      if("Move Position Up".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionNumber " +
                                         "FROM positions " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               positionNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(positionNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PositionName FROM positions " +
                                                "WHERE PositionNumber=" + positionNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the position list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positions " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE positions " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PositionNumber=" + positionNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move positions : " + error,false,context);	
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No positions selected");
          }
        success=true;
        }
      if("Move Position Down".equals(action))
        {
        boolean moveRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionNumber " +
                                         "FROM positions " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               positionNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(positionNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT Sequence,PositionName FROM positions " +
                                                "WHERE PositionNumber=" + positionNumber);
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the position list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positions " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE Type=" + type + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);		
                   }
                 updates=sql1.executeUpdate("UPDATE positions " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionNumber=" + positionNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (current), reason unknown",false,context);		
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move positions : " + error,false,context);		
          }
        if(moveRequested)
          {
          session.setAttribute("message","Move successfull");
          }
        else
          {
          session.setAttribute("message","No positions selected");
          }
        success=true;
        }
      if("Store New Position".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO positions (" +
                                    "PositionNumber,Type," +
                                    "Sequence,PositionName,PositionCode,SubType,PositionDescription,RealPosition,ChartOnly,MaxRookieSalary,MaxFreeAgentSalary,ShirtMin1,ShirtMax1,ShirtMin2,ShirtMax2,DateTimeStamp) " +
                                    "VALUES (" +
                                    positionNumber + "," +
                                    type + "," +
                                    (sequence+1) + ",\"" +
                                    positionName + "\",'" +
                                    positionCode + "'," +
                                    subType + ",'" +
                                    positionDescription + "'," +
                                    realPosition + "," +
                                    chartOnly + "," +
                                    maxRookieSalary + "," +
                                    maxFreeAgentSalary + "," +
                                    shirtMin1 + "," +
                                    shirtMax1 + "," +
                                    shirtMin2 + "," +
                                    shirtMax2 + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New position not created, reason unknown",false,context);		
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create positions : " + error,false,context);		
          }
        //Write default entries into PositionDepthCharts table.  
        int positions[]=null;
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT COUNT(PositionNumber) FROM positions WHERE RealPosition=1 AND Type!=3;");
		  queryResult.first();
		  int numOfPositions=queryResult.getInt(1);
		  positions=new int[numOfPositions];
		  queryResult=sql.executeQuery("SELECT PositionNumber FROM positions WHERE RealPosition=1 AND Type!=3;");
		  int currentPosition=0;
		  while(queryResult.next())
		    {
		    positions[currentPosition]=queryResult.getInt(1);	
		    currentPosition++;	
		    }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to get positiondepthchart data : " + error,false,context);		
		  }          
		for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
		   {
	        try
		       {
		       int updates=0;
		       Statement sql=database.createStatement();
		       ResultSet queryResult;
		       updates=sql.executeUpdate("INSERT INTO positiondepthcharts (" +
									     "PositionNumber,DepthChartPosition,Sequence,DateTimeStamp) " +
									     "VALUES (" +
									     positionNumber + "," +
									     positions[currentPosition] + "," +
									     (currentPosition+1) + ",'" +
									     Routines.getDateTime(false) + "')");
			   if(updates!=1)
			     {
			     Routines.writeToLog(servletName,"New positiondepthchart not created, reason unknown",false,context);		
			     }
			   }
		     catch(SQLException error)
			   {
			   Routines.writeToLog(servletName,"Unable to create positiondepthcharts : " + error,false,context);		
			   }
		   }    
        session.setAttribute("message",positionName + " positions stored successfully");
        success=true;
        }
      if("Store Changed Position".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE positions " +
                                    "SET PositionName='" + positionName + "'," +
                                    "PositionCode='" + positionCode + "',SubType=" +
                                    subType + ",PositionDescription='" + positionDescription +
                                    "',RealPosition=" + realPosition +
                                    ",ChartOnly=" + chartOnly +
                                    ",MaxRookieSalary=" + maxRookieSalary +
                                    ",MaxFreeAgentSalary=" + maxFreeAgentSalary +
                                    ",ShirtMin1=" + shirtMin1 +
                                    ",ShirtMax1=" + shirtMax1 +
                                    ",ShirtMin2=" + shirtMin2 +
                                    ",ShirtMax2=" + shirtMax2 +
                                    ",DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                    "WHERE PositionNumber=" + positionNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Position not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update positions : " + error,false,context);	
          }
        session.setAttribute("message",positionName + " position changed successfully");
        success=true;
        }
      if("Delete Position".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT PositionNumber " +
                                         "FROM positions " +
                                         "WHERE Type=" + type);
          while(queryResult1.next())
               {
               positionNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(positionNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT FormationNumber " +
                                                "FROM formations " +
                                                "WHERE Position1=" + positionNumber + " " +
                                                "OR Position2=" + positionNumber + " " +
                                                "OR Position3=" + positionNumber + " " +
                                                "OR Position4=" + positionNumber + " " +
                                                "OR Position5=" + positionNumber + " " +
                                                "OR Position6=" + positionNumber + " " +
                                                "OR Position7=" + positionNumber + " " +
                                                "OR Position8=" + positionNumber + " " +
                                                "OR Position9=" + positionNumber + " " +
                                                "OR Position10=" + positionNumber + " " +
                                                "OR Position11=" + positionNumber + " " +
                                                "OR Position12=" + positionNumber + " " +
                                                "OR Position13=" + positionNumber + " " +
                                                "OR Position14=" + positionNumber + " " +
                                                "OR Position15=" + positionNumber);
                 if(queryResult2.first())
                   {
                   session.setAttribute("message","Position currently in use by formation entries");
                   return false;
                   }
                 else
                   {
                   queryResult2=sql2.executeQuery("SELECT players.PlayerNumber " +
                                                  "FROM players,masterplayers " +
                                                  "WHERE PositionNumber=" + positionNumber + " " +
                                                  "AND players.MasterPlayerNumber=masterplayers.MasterPlayerNumber");
                   if(queryResult2.first())
                     {
                     session.setAttribute("message","Position currently in use by players entries");
                     return false;
                     }
                 else
                     {
					 updates=sql2.executeUpdate("DELETE FROM positiondepthcharts " +
												"WHERE PositionNumber=" + positionNumber);
                     updates=sql2.executeUpdate("DELETE FROM positions " +
                                                "WHERE PositionNumber=" + positionNumber);
                     if(updates!=1)
                       {
				       Routines.writeToLog(servletName,"Position not deleted (" + positionNumber + ")",false,context);	
                       }
                     }
                   }
                 }
               }
          queryResult1=sql1.executeQuery("SELECT PositionNumber " +
                                         "FROM positions " +
                                         "WHERE Type=" + type + " " +
                                         "ORDER BY Sequence ASC");
          int newSequence=0;
          positionNumber=0;
          while(queryResult1.next())
                {
                newSequence++;
                positionNumber=queryResult1.getInt(1);
                updates=sql2.executeUpdate("UPDATE positions " +
                                           "SET Sequence=" + newSequence + ",DateTimeStamp='" +
                                           Routines.getDateTime(false) + "' " +
                                           "WHERE PositionNumber=" + positionNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position entry not reset (" + positionNumber + ")",false,context);	
                   }
                }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete positions : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No positions selected");
          }
        success=true;
        }
      return success;
      }
}