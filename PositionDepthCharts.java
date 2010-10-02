import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class PositionDepthCharts extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="PositionDepthCharts";

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
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int leagueNumber=Routines.safeParseInt(request.getParameter("leagueNumber"));
      int teamNumber=Routines.safeParseInt(request.getParameter("teamNumber"));
      String action=request.getParameter("action");
      String[] positions=null;
      int[] positionNumbers=null;
      String positionName="";
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
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
      Routines.WriteHTMLHead("Position Depth Charts",//title
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
      int positionSkillsNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        int numOfPositions=0;
        queryResult=sql.executeQuery("SELECT PositionNumber,PositionName " +
                                     "FROM positions " +
                                     "WHERE ( RealPosition=1 OR PositionNumber=16) " +
                                     "AND Type!=3 " +
                                     "ORDER BY PositionNumber ASC");
        while(queryResult.next())
             {
             if(positionNumber==0)
               {
               positionNumber=queryResult.getInt(1);
               }
             numOfPositions++;
             }
        positions=new String[numOfPositions];
        positionNumbers=new int[numOfPositions];
        if(numOfPositions>0)
          {
          numOfPositions=0;
          queryResult.beforeFirst();
          while(queryResult.next())
               {
               positionNumbers[numOfPositions]=queryResult.getInt(1);
               positions[numOfPositions]=queryResult.getString(2);
               numOfPositions++;
               }
          }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to find positions entries : " + error,false,context);	
        }
      boolean updated=true;
      if ("Move Position Up".equals(action)||
          "Move Position Down".equals(action))
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
                             "/servlet/PositionDepthCharts\" METHOD=\"POST\">");
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"positionNumber\">");
      String selectedString="";
      for(int currentPosition=0;currentPosition<positions.length;currentPosition++)
         {
         if(positionNumbers[currentPosition]==positionNumber)
           {
           selectedString=" SELECTED";
           positionName=positions[currentPosition];
           }
         else
           {
           selectedString="";
           }
         webPageOutput.println(" <OPTION" + selectedString + " VALUE=\"" + positionNumbers[currentPosition] + "\">" + positions[currentPosition]);
         }
      webPageOutput.println("</SELECT>");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" NAME=\"action\" VALUE=\"View\">");
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
      int numOfPositions=0;
      int chartPositionNumber=0;
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader(positionName + " Depth Chart Order",2,webPageOutput);
      boolean positionsFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT PositionDepthChartNumber,PositionName " +
                                     "FROM positiondepthcharts,positions " +
                                     "WHERE positiondepthcharts.PositionNumber=" + positionNumber + " " +
                                     "AND positiondepthcharts.DepthChartPosition=positions.PositionNumber " +
                                     "ORDER BY positiondepthcharts.Sequence ASC");
        chartPositionNumber=0;
        positionName="";
        while(queryResult.next())
             {
             numOfPositions++;
             if(!positionsFound)
               {
               positionsFound=true;
               }
             chartPositionNumber=queryResult.getInt(1);
             positionName=queryResult.getString(2);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(chartPositionNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + chartPositionNumber  + "\" VALUE=\"true\"");
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
        webPageOutput.println("No positions found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      if(positionsFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Up\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Position Down\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
	  pool.returnConnection(database);
	  database=null;
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int positionNumber=Routines.safeParseInt(request.getParameter("positionNumber"));
      int sequence=0;
      String skillName="";
	  try
		{
		// Get Latest SequenceNumber.
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT Sequence " +
									 "FROM positiondepthcharts " +
									 "WHERE PositionNumber=" + positionNumber + " " +
									 "ORDER BY Sequence DESC");
		if(queryResult.first())
		  {
		  sequence=queryResult.getInt(1);
		  }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to retrieve positiondepthcharts : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT PositionDepthChartNumber " +
                                         "FROM positiondepthcharts " +
                                         "WHERE PositionNumber=" + positionNumber + " " +
                                         "ORDER BY Sequence ASC");
          while(queryResult1.next())
               {
               int chartPositionNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(chartPositionNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT positiondepthcharts.Sequence,PositionName FROM positiondepthcharts,positions " +
                                                "WHERE PositionDepthChartNumber=" + chartPositionNumber + " " +
                                                "AND positiondepthcharts.DepthChartPosition=positions.PositionNumber");
                 queryResult2.first();
				 System.out.println("Sequence2="+currentSequence);
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==1)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the chart");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positiondepthcharts " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionNumber=" + positionNumber + " " +
                                            "AND Sequence=" + (currentSequence-1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE positiondepthcharts " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false)  + "' " +
                                            "WHERE PositionDepthChartNumber=" + chartPositionNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (current), reason unknown",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to move position : " + error,false,context);	
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
          queryResult1=sql1.executeQuery("SELECT PositionDepthChartNumber " +
                                         "FROM positiondepthcharts " +
                                         "WHERE PositionNumber=" + positionNumber + " " +
                                         "ORDER BY Sequence DESC");
          while(queryResult1.next())
               {
               int chartPositionNumber=queryResult1.getInt(1);
               int currentSequence=0;
               if("true".equals(request.getParameter(String.valueOf(chartPositionNumber))))
                 {
                 if(!moveRequested)
                   {
                   moveRequested=true;
                   }
                 queryResult2=sql2.executeQuery("SELECT positiondepthcharts.Sequence,PositionName FROM positiondepthcharts,positions " +
                                                "WHERE PositionDepthChartNumber=" + chartPositionNumber + " " +
                                                "AND positiondepthcharts.DepthChartPosition=positions.PositionNumber");
                 queryResult2.first();
                 currentSequence=queryResult2.getInt(1);
                 if(currentSequence==sequence)
                   {
                   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the position list");
                   return false;
                   }
                 updates=sql1.executeUpdate("UPDATE positiondepthcharts " +
                                            "SET Sequence=(Sequence-1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionNumber=" + positionNumber + " " +
                                            "AND Sequence=" + (currentSequence+1));
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"Position not moved (prior), reason unknown",false,context);	
                   }
                 updates=sql1.executeUpdate("UPDATE positiondepthcharts " +
                                            "SET Sequence=(Sequence+1),DateTimeStamp='" +
                                            Routines.getDateTime(false) + "' " +
                                            "WHERE PositionDepthChartNumber=" + chartPositionNumber);
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
      return success;
      }
}