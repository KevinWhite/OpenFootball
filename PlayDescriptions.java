import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class PlayDescriptions extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="PlayDescriptions";

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
	  if("New Description".equals(action)||
		 "Change Description".equals(action))
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
	  if(Routines.loginCheck(true,request,response,database,context))
		{
		return;
		}
	  Routines.WriteHTMLHead("Play Descriptions",//title
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
	  int descriptionNumber=0;
	  webPageOutput.println("<CENTER>");
	  webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
							" WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
	  webPageOutput.println("</CENTER>");
	  if("Change Description".equals(action))
		{
		boolean changeRequested=false;
		int changeCount=0;
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT DescriptionNumber " +
									   "FROM playdescriptions ");
		  while(queryResult.next())
			   {
			   descriptionNumber=queryResult.getInt(1);
			   if("true".equals(request.getParameter(String.valueOf(descriptionNumber))))
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
		  Routines.writeToLog(servletName,"Unable to find playdescription entries : " + error,false,context);	
		  }
		if(!changeRequested)
		  {
		  session.setAttribute("message","No description selected");
		  action="";
		  }
		if(changeCount>1)
		  {
		  session.setAttribute("message","Please select only one description to change");
		  action="";
		  }
		}
	  boolean updated=true;
	  if ("Store New Description".equals(action)||
		  "Store Changed Description".equals(action)||
		  "Delete Description".equals(action)||
		  "Move Description Up".equals(action)||
		  "Move Description Down".equals(action))
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
							 "/servlet/PlayDescriptions\" METHOD=\"POST\">");
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
	  if("New Description".equals(action)||
		 "Change Description".equals(action))
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
	  int descriptionNumber=0;
	  Routines.tableStart(false,webPageOutput);
	  Routines.tableHeader("PlayDescriptions",3,webPageOutput);
	  boolean descriptionsFound=false;
	  try
		{
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT DescriptionNumber,Description " +
									 "FROM playdescriptions " +
									 "ORDER BY Sequence ASC");
		descriptionNumber=0;
		String description="";
		while(queryResult.next())
			 {
			 if(!descriptionsFound)
			   {
				descriptionsFound=true;
			   }
			 descriptionNumber=queryResult.getInt(1);
			 description=queryResult.getString(2);
			 Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
			 boolean selected=false;
			 String param="";
			 if(!updated)
			   {
			   param=request.getParameter(String.valueOf(descriptionNumber));
			   if("true".equals(param))
				 {
				 selected=true;
				 }
			   }
			 webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + descriptionNumber  + "\" VALUE=\"true\"");
			 if(selected)
			   {
			   webPageOutput.print(" CHECKED");
			   }
			 webPageOutput.println(">");
			 Routines.tableDataEnd(false,false,false,webPageOutput);
			 Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			 webPageOutput.println(descriptionNumber);
			 Routines.tableDataEnd(false,false,false,webPageOutput);
			 Routines.tableDataStart(true,false,false,false,false,90,0,"scoresrow",webPageOutput);
			 webPageOutput.println(description);
			 Routines.tableDataEnd(false,false,true,webPageOutput);
			 }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to retrieve playdescription : " + error,false,context);		
		}
	  if(!descriptionsFound)
		{
		Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
		webPageOutput.println("No Descriptions found.");
		Routines.tableDataEnd(false,false,true,webPageOutput);
		}
	  Routines.tableEnd(webPageOutput);
	  webPageOutput.println(Routines.spaceLines(1));
	  Routines.tableStart(false,webPageOutput);
	  Routines.tableHeader("Actions",0,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Description\" NAME=\"action\">");
	  if(descriptionsFound)
		{
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Description\" NAME=\"action\">");
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Description\" NAME=\"action\">");
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Description Up\" NAME=\"action\">");
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Move Description Down\" NAME=\"action\">");
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
	  int descriptionNumber=0;
	  String description="";
	  Routines.tableStart(false,webPageOutput);
	  if("Change Description".equals(action))
		{
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  queryResult=sql.executeQuery("SELECT DescriptionNumber " +
									   "FROM playdescriptions " +
									   "ORDER BY Sequence DESC");
		  int tempDescriptionNumber=0;
		  while(queryResult.next())
			   {
			   tempDescriptionNumber=queryResult.getInt(1);
			   int currentSequence=0;
			   if("true".equals(request.getParameter(String.valueOf(tempDescriptionNumber))))
				 {
				 queryResult=sql.executeQuery("SELECT DescriptionNumber,Description " +
											  "FROM playdescriptions " +
											  "WHERE DescriptionNumber=" + tempDescriptionNumber);
				 if(queryResult.first())
				   {
				   descriptionNumber=queryResult.getInt(1);
				   description=queryResult.getString(2);
				   }
				 else
				   {
				   Routines.writeToLog(servletName,"Unable to find playdescription (" + tempDescriptionNumber + ")",false,context);	
				   }
				 }
			   }
			}
	   catch(SQLException error)
			{
			Routines.writeToLog(servletName,"Unable to retrieve playdescription : " + error,false,context);	
			}
	  Routines.tableHeader("Amend details of description",2,webPageOutput);
	  }
	  if("New Description".equals(action))
		{
		Routines.tableHeader("Enter details of new description",2,webPageOutput);
		}
	  Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
	  webPageOutput.print("Description");
	  Routines.tableDataEnd(false,false,false,webPageOutput);
	  Routines.tableDataStart(true,false,false,false,false,75,0,"scoresrow",webPageOutput);
	  webPageOutput.print("<INPUT TYPE=\"TEXT\" NAME=\"descriptionName\" SIZE=\"30\" MAXLENGTH=\"30\" VALUE=\"" + description + "\">");
	  Routines.tableDataEnd(false,false,true,webPageOutput);
	  Routines.tableEnd(webPageOutput);
	  webPageOutput.println(Routines.spaceLines(1));
	  Routines.tableStart(false,webPageOutput);
	  Routines.tableHeader("Actions",1,webPageOutput);
	  Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
	  if("New Description".equals(action))
		{
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Description\" NAME=\"action\">");
		}
	  else
		{
		webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Description\" NAME=\"action\">");
		}
	  webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
	  Routines.tableDataEnd(false,false,true,webPageOutput);
	  Routines.tableEnd(webPageOutput);
	  webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
	  webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"skillNumber\" VALUE=\"" + descriptionNumber + "\">");
	  webPageOutput.println("</FORM>");
	  }

   private synchronized boolean updateEntry(String action,
											HttpSession session,
											HttpServletRequest request,
											Connection database)
	  {
	  boolean success=false;
	  int descriptionNumber=Routines.safeParseInt(request.getParameter("skillNumber"));
	  int sequence=0;
	  String description=request.getParameter("descriptionName");
	  try
		{
		// Get Latest SequenceNumber.
		Statement sql=database.createStatement();
		ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT Sequence " +
									 "FROM playdescriptions " +
									 "ORDER BY Sequence DESC");
		if(queryResult.first())
		  {
		  sequence=queryResult.getInt(1);
		  }
		if(descriptionNumber==0)
		  {
		  //Get latest skillNumber.
		  descriptionNumber=1;
		  queryResult=sql.executeQuery("SELECT DescriptionNumber " +
									   "FROM playdescriptions " +
									   "ORDER BY DescriptionNumber DESC");
		  if(queryResult.first())
			{
			descriptionNumber=queryResult.getInt(1) + 1;
			}
		  }
		}
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Unable to retrieve skills : " + error,false,context);	
		}
	  if("Move Description Up".equals(action))
		{
		boolean moveRequested=false;
		try
		  {
		  int updates=0;
		  Statement sql1=database.createStatement();
		  Statement sql2=database.createStatement();
		  ResultSet queryResult1;
		  ResultSet queryResult2;
		  queryResult1=sql1.executeQuery("SELECT DescriptionNumber " +
										 "FROM playdescriptions " +
										 "ORDER BY Sequence ASC");
		  while(queryResult1.next())
			   {
			   descriptionNumber=queryResult1.getInt(1);
			   int currentSequence=0;
			   if("true".equals(request.getParameter(String.valueOf(descriptionNumber))))
				 {
				 if(!moveRequested)
				   {
				   moveRequested=true;
				   }
				 queryResult2=sql2.executeQuery("SELECT Sequence,Description FROM playdescriptions " +
												"WHERE DescriptionNumber=" + descriptionNumber);
				 queryResult2.first();
				 currentSequence=queryResult2.getInt(1);
				 if(currentSequence==1)
				   {
				   session.setAttribute("message",queryResult2.getString(2) + " is already at the top of the description list");
				   return false;
				   }
				 updates=sql1.executeUpdate("UPDATE playdescriptions " +
											"SET Sequence=(Sequence+1),DateTimeStamp='" +
											Routines.getDateTime(false) + "' " +
											"WHERE Sequence=" + (currentSequence-1));
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"Description not moved (prior), reason unknown",false,context);	
				   }
				 updates=sql1.executeUpdate("UPDATE playdescriptions " +
											"SET Sequence=(Sequence-1),DateTimeStamp='" +
											Routines.getDateTime(false)  + "' " +
											"WHERE DescriptionNumber=" + descriptionNumber);
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"Description not moved (current), reason unknown",false,context);	
				   }
				 }
			   }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to move descriptions : " + error,false,context);	
		  }
		if(moveRequested)
		  {
		  session.setAttribute("message","Move successfull");
		  }
		else
		  {
		  session.setAttribute("message","No descriptions selected");
		  }
		success=true;
		}
	  if("Move Description Down".equals(action))
		{
		boolean moveRequested=false;
		try
		  {
		  int updates=0;
		  Statement sql1=database.createStatement();
		  Statement sql2=database.createStatement();
		  ResultSet queryResult1;
		  ResultSet queryResult2;
		  queryResult1=sql1.executeQuery("SELECT DescriptionNumber " +
										 "FROM playdescriptions " +
										 "ORDER BY Sequence DESC");
		  while(queryResult1.next())
			   {
			   descriptionNumber=queryResult1.getInt(1);
			   int currentSequence=0;
			   if("true".equals(request.getParameter(String.valueOf(descriptionNumber))))
				 {
				 if(!moveRequested)
				   {
				   moveRequested=true;
				   }
				 queryResult2=sql2.executeQuery("SELECT Sequence,Description FROM playdescriptions " +
												"WHERE DescriptionNumber=" + descriptionNumber);
				 queryResult2.first();
				 currentSequence=queryResult2.getInt(1);
				 if(currentSequence==sequence)
				   {
				   session.setAttribute("message",queryResult2.getString(2) + " is already at the bottom of the description list");
				   return false;
				   }
				 updates=sql1.executeUpdate("UPDATE playdescriptions " +
											"SET Sequence=(Sequence-1),DateTimeStamp='" +
											Routines.getDateTime(false) + "' " +
											"WHERE Sequence=" + (currentSequence+1));
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"Description not moved (prior), reason unknown",false,context);	
				   }
				 updates=sql1.executeUpdate("UPDATE playdescriptions " +
											"SET Sequence=(Sequence+1),DateTimeStamp='" +
											Routines.getDateTime(false) + "' " +
											"WHERE DescriptionNumber=" + descriptionNumber);
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"Description not moved (current), reason unknown",false,context);	
				   }
				 }
			   }
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to move descriptions : " + error,false,context);	
		  }
		if(moveRequested)
		  {
		  session.setAttribute("message","Move successfull");
		  }
		else
		  {
		  session.setAttribute("message","No descriptions selected");
		  }
		success=true;
		}
	  if("Store New Description".equals(action))
		{
		try
		  {
		  int updates=0;
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  updates=sql.executeUpdate("INSERT INTO playdescriptions (" +
									"DescriptionNumber,Sequence,Description,DateTimeStamp) " +
									"VALUES (" +
									descriptionNumber + "," +
									(sequence+1) + ",\"" +
									description + "\",'" +
									Routines.getDateTime(false) + "')");
		  if(updates!=1)
			{
			Routines.writeToLog(servletName,"New description not created, reason unknown",false,context);	
			}
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to create description : " + error,false,context);	
		  }
		session.setAttribute("message",description + " skill stored successfully");
		success=true;
		}
	  if("Store Changed Description".equals(action))
		{
		try
		  {
		  int updates=0;
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
		  updates=sql.executeUpdate("UPDATE playdescriptions " +
									"SET Description='" + description + "'," +
									"DateTimeStamp='" + Routines.getDateTime(false) + "' " +
									"WHERE DescriptionNumber=" + descriptionNumber);
		  if(updates!=1)
			{
			Routines.writeToLog(servletName,"Description not updated, reason unknown",false,context);	
			}
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to update descriptions : " + error,false,context);	
		  }
		session.setAttribute("message",description + " skill changed successfully");
		success=true;
		}
	  if("Delete Description".equals(action))
		{
		boolean deleteRequested=false;
		try
		  {
		  int updates=0;
		  Statement sql1=database.createStatement();
		  Statement sql2=database.createStatement();
		  ResultSet queryResult1;
		  ResultSet queryResult2;
		  queryResult1=sql1.executeQuery("SELECT DescriptionNumber " +
										 "FROM playdescriptions");
		  while(queryResult1.next())
			   {
			   descriptionNumber=queryResult1.getInt(1);
			   if("true".equals(request.getParameter(String.valueOf(descriptionNumber))))
				 {
				 if(!deleteRequested)
				   {
				   deleteRequested=true;
				   }
				 queryResult2=sql2.executeQuery("SELECT Description01 " +
												"FROM playbyplay " +
												"WHERE Description01=" + descriptionNumber);
				 if(queryResult2.first())
				   {
				   session.setAttribute("message","Description currently in use by playbyplay(01) entries");
				   return false;
				   }
				 else
				   {
				   queryResult2=sql2.executeQuery("SELECT Description02 " +
												  "FROM playbyplay " +
												  "WHERE Description02=" + descriptionNumber);
				   if(queryResult2.first())
					 {
					 session.setAttribute("message","Description currently in use by playbyplay(02) entries");
					 return false;
					 }
				 else
					 {
					 updates=sql2.executeUpdate("DELETE FROM playdescriptions " +
												"WHERE DescriptionNumber=" + descriptionNumber);
					   if(updates!=1)
						 {
						 Routines.writeToLog(servletName,"Description not deleted (" + descriptionNumber + ")",false,context);	
						 }
					 }
				   }
				 }
			   }
		  queryResult1=sql1.executeQuery("SELECT DescriptionNumber " +
										 "FROM playdescriptions " +
										 "ORDER BY Sequence ASC");
		  int newSequence=0;
		  descriptionNumber=0;
		  while(queryResult1.next())
				{
				newSequence++;
				descriptionNumber=queryResult1.getInt(1);
				updates=sql2.executeUpdate("UPDATE playdescriptions " +
										   "SET Sequence=" + newSequence + ",DateTimeStamp='" +
										   Routines.getDateTime(false) + "' " +
										   "WHERE DescriptionNumber=" + descriptionNumber);
				 if(updates!=1)
				   {
				   Routines.writeToLog(servletName,"Description entry not reset (" + descriptionNumber + ")",false,context);	
				   }
				}
		  }
		catch(SQLException error)
		  {
		  Routines.writeToLog(servletName,"Unable to delete descriptions : " + error,false,context);	
		  }
		if(deleteRequested)
		  {
		  session.setAttribute("message","Delete successfull");
		  }
		else
		  {
		  session.setAttribute("message","No descriptions selected");
		  }
		success=true;
		}
	  return success;
	  }
}