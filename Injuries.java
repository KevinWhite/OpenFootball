import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Injuries extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Injuries";

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
      if("New Injury".equals(action)||
         "Change Injury".equals(action))
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
      Routines.WriteHTMLHead("Injuries",//title
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
      int injuryNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Injury".equals(action))
        {
        boolean changeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT InjuryNumber " +
                                       "FROM injuries");
          while(queryResult.next())
               {
               injuryNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(injuryNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find injury entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No injury selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one injury to change");
          disabledText="";
          action="";
          }
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/Injuries\" METHOD=\"POST\">");
      if("New Injury".equals(action)||
         "Change Injury".equals(action))
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
      int injuryNumber=0;
      if ("Store New Injury".equals(action)||
          "Store Changed Injury".equals(action)||
          "Delete Injury".equals(action))
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
      Routines.tableHeader("Injuries",3,webPageOutput);
      boolean injuryFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
        queryResult=sql.executeQuery("SELECT InjuryNumber,PassNumber,Length " +
                                     "FROM injuries " +
                                     "ORDER BY PassNumber DESC");
        injuryNumber=0;
        while(queryResult.next())
             {
             if(!injuryFound)
               {
               injuryFound=true;
               }
             injuryNumber=queryResult.getInt(1);
             int passNumber=queryResult.getInt(2);
             int length=queryResult.getInt(3);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(injuryNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + injuryNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
			 Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			 webPageOutput.println(passNumber);
			 Routines.tableDataEnd(false,false,false,webPageOutput);
             Routines.tableDataStart(true,false,false,false,false,90,0,"scoresrow",webPageOutput);
             webPageOutput.println(getInjuryText(length));
             Routines.tableDataEnd(false,false,true,webPageOutput);
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve injuries : " + error,false,context);	
        }
      if(!injuryFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Injuries found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Injury\" NAME=\"action\">");
      if(injuryFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Injury\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Injury\" NAME=\"action\">");
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
      int[] passNumbers=null;	
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT PassNumber " +
								     "FROM injuries " +
									 "ORDER BY PassNumber DESC");
		int numOfInjuries=0;
		while(queryResult.next())
		      {
		      numOfInjuries++;	
		      }	
		queryResult.beforeFirst();
		passNumbers=new int[numOfInjuries];
		numOfInjuries=0;
		while(queryResult.next())   
			 {
			 passNumbers[numOfInjuries]=queryResult.getInt(1);
			 numOfInjuries++;
			 }

		}
      catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to retrieve injuries : " + error,false,context);	
		}		      	
      int injuryNumber=0;
      String injuryText="";
      int passNumber=0;
      int length=0;
      Routines.tableStart(false,webPageOutput);
      if("Change Injury".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT InjuryNumber " +
                                       "FROM injuries " +
                                       "ORDER BY PassNumber DESC");
          int tempInjuryNumber=0;
          while(queryResult.next())
               {
               tempInjuryNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(tempInjuryNumber))))
                 {
                 queryResult=sql.executeQuery("SELECT InjuryNumber,PassNumber,Length " +
                                              "FROM injuries " +
                                              "WHERE InjuryNumber=" + tempInjuryNumber);
                 if(queryResult.first())
                   {
                   injuryNumber=queryResult.getInt(1);
                   passNumber=queryResult.getInt(2);
                   length=queryResult.getInt(3);
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find injury (" + tempInjuryNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve injury: " + error,false,context);	
            }
      Routines.tableHeader("Amend details of Injury",2,webPageOutput);
      }
      if("New Injury".equals(action))
        {
        Routines.tableHeader("Enter details of new Injury",2,webPageOutput);
        }
      Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
      webPageOutput.print("Pass Number");
      Routines.tableDataEnd(false,false,false,webPageOutput);
      Routines.tableDataStart(true,false,false,false,false,30,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"passNumber\">");
	  String selected="";
	  for(int currentNumber=1;currentNumber<49;currentNumber++)
		 {
		 boolean used=false;	
		 for(int currentPassNumber=0;currentPassNumber<passNumbers.length;currentPassNumber++)
		    {	
		    if(passNumbers[currentPassNumber]==currentNumber)
		      {
		      used=true;
		      currentPassNumber=passNumbers.length;		
		      }
		    }
		 if(!used||currentNumber==passNumber)
		   {   
		   if(currentNumber==passNumber)
		     {
		     selected=" SELECTED";
		     }
		   else
		     {
		     selected="";
		     }
		   webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + currentNumber);
		   }
		 }  
	  webPageOutput.println("</SELECT>");
	  Routines.tableDataEnd(false,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
	  webPageOutput.print("Injury Length");
	  Routines.tableDataEnd(false,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,false,false,false,40,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<SELECT NAME=\"injuryLength\">");
	  selected="";
	  for(int currentNumber=1;currentNumber<48;currentNumber++)
		 {
		 if(length<25)
		   {
		   length*=-1;	
		   }
		 else
		   {
		   length-=24;	  
		   }
		 if(currentNumber==length)
		   {
		   selected=" SELECTED";
		   }
		 else
		   {
		   selected="";
		   }
		 int tempCurrentNumber=currentNumber;
		 if(tempCurrentNumber<25)
		   {
		   tempCurrentNumber*=-1;	
		   }
		 else
		   {
		   tempCurrentNumber-=24;	  
		   }		   
		 webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + getInjuryText(tempCurrentNumber));
		 }
	  webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Injury".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Injury\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Injury\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"injuryNumber\" VALUE=\"" + injuryNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private String getInjuryText(int length)
      {
      System.out.println("lengtPre="+length);	
	  if(length<0)
		{
		length*=-1;	
		}
	  else
		{
		length+=24;	  
		}
	  System.out.println("lengtPost="+length);	
      String lengthText="";	
      if(length<25)
		{
		if(length<21)
		  {
		  lengthText="Next " + length + " play";
		  if(length!=1)
			{
			lengthText+="s";		
			}
		  }
		else
		  {
		  if(length==21)
			{
			lengthText="Rest of possession";		    
			}
		  if(length==22)
			{
			lengthText="Rest of quarter";		    
			}
		  if(length==23)
			{
			lengthText="Rest of half";		    
			}
		  if(length==24)
			{
			lengthText="Rest of game";		    
			}				    				                      
		  }
		}
	   else
		{	
		if(length==47)
		  {
		  lengthText="Season";		
		  }
		else
		  {
		  lengthText="Next " + (length-24) + " game";
		  if(length!=25)
		    {
			lengthText+="s";		  
			}
		  }
		}
	  return lengthText;	      	
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int injuryNumber=Routines.safeParseInt(request.getParameter("injuryNumber"));
	  int passNumber=Routines.safeParseInt(request.getParameter("passNumber"));
	  int injuryLength=Routines.safeParseInt(request.getParameter("injuryLength"));
	  if(injuryLength<25)
	    {
	    injuryLength*=-1;	
	    }
	  else
	    {
	    injuryLength-=24;	  
	    }
      if(injuryNumber==0)
        {
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
          //Get latest injuryNumber.
          injuryNumber=1;
          queryResult=sql.executeQuery("SELECT InjuryNumber " +
                                       "FROM injuries " +
                                       "ORDER BY InjuryNumber DESC");
          if(queryResult.first())
            {
            injuryNumber=queryResult.getInt(1) + 1;
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to retrieve injuries : " + error,false,context);	
          }
        }  
      if("Store New Injury".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("INSERT INTO injuries (" +
                                    "InjuryNumber,PassNumber,Length,DateTimeStamp) " +
                                    "VALUES (" +
                                    injuryNumber + "," +
                                    passNumber + "," +
                                    injuryLength + ",'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New injury not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create injuries : " + error,false,context);
          }
        session.setAttribute("message",injuryNumber + " injury stored successfully");
        success=true;
        }
      if("Store Changed Injury".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE injuries " +
                                    "SET PassNumber=" + passNumber + "," +
                                    "Length=" + injuryLength + "," +
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE InjuryNumber=" + injuryNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Injury not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update injuries : " + error,false,context);	
          }
        session.setAttribute("message",injuryNumber + " injury changed successfully");
        success=true;
        }
      if("Delete Injury".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT InjuryNumber " +
                                         "FROM injuries");
          while(queryResult1.next())
               {
               injuryNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(injuryNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM injuries " +
                                            "WHERE InjuryNumber=" + injuryNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"injury not deleted (" + injuryNumber + ")",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete injuries : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No injury selected");
          }
        success=true;
        }
      return success;
      }
}