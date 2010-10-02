import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ActionCards extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Action Cards";
   private static int sizeOfCardArray=92;
   private static String[] cardItemText=new String[sizeOfCardArray];

   public void init()
      {
	  cardItemText[0]="Normal";
	  cardItemText[1]="RunNo";
	  cardItemText[2]="OutOfBounds";
	  cardItemText[3]="PassNumber";
	  cardItemText[4]="SweepLeftBreak";
	  cardItemText[5]="SweepLeftLE";
	  cardItemText[6]="SweepLeftLT";
	  cardItemText[7]="SweepLeftLG";
	  cardItemText[8]="SweepLeftCN";
	  cardItemText[9]="SweepLeftRG";
	  cardItemText[10]="SweepLeftRT";
	  cardItemText[11]="SweepLeftRE";
	  cardItemText[12]="SweepLeftBK";
	  cardItemText[13]="SweepLeftLDE";
	  cardItemText[14]="SweepLeftLDT";
	  cardItemText[15]="SweepLeftNT";
	  cardItemText[16]="SweepLeftRDT";
	  cardItemText[17]="SweepLeftRDE";
	  cardItemText[18]="SweepLeftLOLB";
	  cardItemText[19]="SweepLeftLILB";
	  cardItemText[20]="SweepLeftMLB";
	  cardItemText[21]="SweepLeftRILB";
	  cardItemText[22]="SweepLeftROLB";
	  cardItemText[23]="InsideLeftBreak";
	  cardItemText[24]="InsideLeftLE";
	  cardItemText[25]="InsideLeftLT";
	  cardItemText[26]="InsideLeftLG";
	  cardItemText[27]="InsideLeftCN";
	  cardItemText[28]="InsideLeftRG";
	  cardItemText[29]="InsideLeftRT";
	  cardItemText[30]="InsideLeftRE";
	  cardItemText[31]="InsideLeftBK";
	  cardItemText[32]="InsideLeftLDE";
	  cardItemText[33]="InsideLeftLDT";
	  cardItemText[34]="InsideLeftNT";
	  cardItemText[35]="InsideLeftRDT";
	  cardItemText[36]="InsideLeftRDE";
	  cardItemText[37]="InsideLeftLOLB";
	  cardItemText[38]="InsideLeftLILB";
	  cardItemText[39]="InsideLeftMLB";
	  cardItemText[40]="InsideLeftRILB";
	  cardItemText[41]="InsideLeftROLB";
	  cardItemText[42]="SweepRightBreak";
	  cardItemText[43]="SweepRightLE";
	  cardItemText[44]="SweepRightLT";
	  cardItemText[45]="SweepRightLG";
	  cardItemText[46]="SweepRightCN";
	  cardItemText[47]="SweepRightRG";
	  cardItemText[48]="SweepRightRT";
	  cardItemText[49]="SweepRightRE";
	  cardItemText[50]="SweepRightBK";
	  cardItemText[51]="SweepRightLDE";
	  cardItemText[52]="SweepRightLDT";
	  cardItemText[53]="SweepRightNT";
	  cardItemText[54]="SweepRightRDT";
	  cardItemText[55]="SweepRightRDE";
	  cardItemText[56]="SweepRightLOLB";
	  cardItemText[57]="SweepRightLILB";
	  cardItemText[58]="SweepRightMLB";
	  cardItemText[59]="SweepRightRILB";
	  cardItemText[60]="SweepRightROLB";
	  cardItemText[61]="InsideRightBreak";
	  cardItemText[62]="InsideRightLE";
	  cardItemText[63]="InsideRightLT";
	  cardItemText[64]="InsideRightLG";
	  cardItemText[65]="InsideRightCN";
	  cardItemText[66]="InsideRightRG";
	  cardItemText[67]="InsideRightRT";
	  cardItemText[68]="InsideRightRE";
	  cardItemText[69]="InsideRightBK";
	  cardItemText[70]="InsideRightLDE";
	  cardItemText[71]="InsideRightLDT";
	  cardItemText[72]="InsideRightNT";
	  cardItemText[73]="InsideRightRDT";
	  cardItemText[74]="InsideRightRDE";
	  cardItemText[75]="InsideRightLOLB";
	  cardItemText[76]="InsideRightLILB";
	  cardItemText[77]="InsideRightMLB";
	  cardItemText[78]="InsideRightRILB";
	  cardItemText[79]="InsideRightROLB";
	  cardItemText[80]="EndAround";
	  cardItemText[81]="QuickPass";
	  cardItemText[82]="ShortPass";
	  cardItemText[83]="LongPass";
	  cardItemText[84]="Screen";
	  cardItemText[85]="Injury";
	  cardItemText[86]="Fumble";
	  cardItemText[87]="RunQuickPenalty";
	  cardItemText[88]="ShortLongPenalty";
	  cardItemText[89]="PuntPenalty";
	  cardItemText[90]="KickOffPenalty";	  	  	  	  	
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
      if("New Card".equals(action)||
         "Change Card".equals(action))
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
      Routines.WriteHTMLHead("ActionCards",//title
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
      int cardNumber=0;
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      if("Change Card".equals(action))
        {
        boolean changeRequested=false;
        boolean zChangeRequested=false;
        int changeCount=0;
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT CardNumber,Normal " +
                                       "FROM actioncards");
          while(queryResult.next())
               {
               cardNumber=queryResult.getInt(1);
               int normal=queryResult.getInt(2);
               if("true".equals(request.getParameter(String.valueOf(cardNumber))))
                 {
                 changeCount++;
                 if(!changeRequested)
                   {
                   changeRequested=true;
                   }
                 if(!zChangeRequested&&normal==0)
                   {
                   zChangeRequested=true;	  
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to find actioncard entries : " + error,false,context);	
          }
        if(!changeRequested)
          {
          session.setAttribute("message","No card selected");
          disabledText="";
          action="";
          }
        if(changeCount>1)
          {
          session.setAttribute("message","Please select only one card to change");
          disabledText="";
          action="";
          }
		if(zChangeRequested)
		  {
		  session.setAttribute("message","Z Cards cannot be changed");
		  disabledText="";
		  action="";
		  }          
        }
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/ActionCards\" METHOD=\"POST\">");
      if("New Card".equals(action)||
         "Change Card".equals(action))
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
      int cardNumber=0;
      if ("Store New Card".equals(action)||
          "Store Changed Card".equals(action)||
          "Delete Card".equals(action))
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
      Routines.tableHeader("Cards",5,webPageOutput);
      boolean cardFound=false;
      try
        {
        Statement sql=database.createStatement();
        ResultSet queryResult;
		queryResult=sql.executeQuery("SELECT CardNumber,Normal,RunNumber,PassNumber " +
		                             "FROM actioncards " +
									 "ORDER BY Normal DESC, RunNumber ASC, PassNumber ASC, CardNumber ASC");        

        cardNumber=0;
        while(queryResult.next())
             {
             if(!cardFound)
               {
               cardFound=true;
               }
             cardNumber=queryResult.getInt(1);
             int normal=queryResult.getInt(2);
			 int runNumber=queryResult.getInt(3);
			 int passNumber=queryResult.getInt(4);
             Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
             boolean selected=false;
             String param="";
             if(!updated)
               {
               param=request.getParameter(String.valueOf(cardNumber));
               if("true".equals(param))
                 {
                 selected=true;
                 }
               }
             webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + cardNumber  + "\" VALUE=\"true\"");
             if(selected)
               {
               webPageOutput.print(" CHECKED");
               }
             webPageOutput.println(">");
             Routines.tableDataEnd(false,false,false,webPageOutput);
             if(normal==1)
               {
               Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			   webPageOutput.println("RunNumber");
			   Routines.tableDataEnd(false,false,false,webPageOutput);
			   Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			   webPageOutput.println(runNumber);			 
               Routines.tableDataEnd(false,false,false,webPageOutput);
			   Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			   webPageOutput.println("PassNumber");
			   Routines.tableDataEnd(false,false,false,webPageOutput);
			   Routines.tableDataStart(true,false,false,false,false,5,0,"scoresrow",webPageOutput);
			   webPageOutput.println(passNumber);			 
			   Routines.tableDataEnd(false,false,true,webPageOutput);               
               }
             else
               {  
			   Routines.tableDataStart(true,false,false,false,false,5,4,"scoresrow",webPageOutput);
			   webPageOutput.println("Z Card");			 
			   Routines.tableDataEnd(false,false,true,webPageOutput);
               }
             }
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to retrieve actioncard : " + error,false,context);	
        }
      if(!cardFound)
        {
        Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
        webPageOutput.println("No Cards found.");
        Routines.tableDataEnd(false,false,true,webPageOutput);
        }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"New Card\" NAME=\"action\">");
      if(cardFound)
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Change Card\" NAME=\"action\">");
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Delete Card\" NAME=\"action\">");
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
      int cardNumber=0;
      int[] cardData=new int[sizeOfCardArray];
      Routines.tableStart(false,webPageOutput);
      if("Change Card".equals(action))
        {
        try
          {
          Statement sql=database.createStatement();
          ResultSet queryResult;
          queryResult=sql.executeQuery("SELECT CardNumber " +
                                       "FROM actionCards " +
                                       "ORDER BY CardNumber DESC");
          int tempCardNumber=0;
          while(queryResult.next())
               {
               tempCardNumber=queryResult.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(tempCardNumber))))
                 {
				 queryResult=sql.executeQuery("SELECT CardNumber,Normal,RunNumber,OutOfBounds,PassNumber," +
											  "SweepLeftBreak,SweepLeftLE,SweepLeftLT,SweepLeftLG,SweepLeftCN,SweepLeftRG,SweepLeftRT,SweepLeftRE," +
											  "SweepLeftBK,SweepLeftLDE,SweepLeftLDT,SweepLeftNT,SweepLeftRDT,SweepLeftRDE,SweepLeftLOLB,SweepLeftLILB,SweepLeftMLB,SweepLeftRILB,SweepLeftROLB," +
											  "InsideLeftBreak,InsideLeftLE,InsideLeftLT,InsideLeftLG,InsideLeftCN,InsideLeftRG,InsideLeftRT,InsideLeftRE," +
											  "InsideLeftBK,InsideLeftLDE,InsideLeftLDT,InsideLeftNT,InsideLeftRDT,InsideLeftRDE,InsideLeftLOLB,InsideLeftLILB,InsideLeftMLB,InsideLeftLILB,InsideLeftLOLB," +
											  "SweepRightBreak,SweepRightLE,SweepRightLT,SweepRightLG,SweepRightCN,SweepRightRG,SweepRightRT,SweepRightRE," +
											  "SweepRightBK,SweepRightLDE,SweepRightLDT,SweepRightNT,SweepRightRDT,SweepRightRDE,SweepRightLOLB,SweepRightLILB,SweepRightMLB,SweepRightRILB,SweepRightROLB," +
											  "InsideRightBreak,InsideRightLE,InsideRightLT,InsideRightLG,InsideRightCN,InsideRightRG,InsideRightRT,InsideRightRE," +
											  "InsideRightBK,InsideRightLDE,InsideRightLDT,InsideRightNT,InsideRightRDT,InsideRightRDE,InsideRightLOLB,InsideRightLILB,InsideRightMLB,InsideRightRILB,InsideRightROLB," +
											  "EndAround,QuickPass,ShortPass,LongPass,Screen,Injury,Fumble,RunQuickPenalty,ShortLongPenalty,PuntPenalty,KickOffPenalty " +
											  "FROM actioncards " +
                                              "WHERE CardNumber = " + tempCardNumber + " " +
										      "ORDER BY CardNumber ASC");
                 if(queryResult.first())
                   {
                   cardNumber=queryResult.getInt(1);
 				   cardData=new int[sizeOfCardArray];
				   for(int currentItem=0;currentItem<(sizeOfCardArray-1);currentItem++)
					  {
					  cardData[currentItem]=queryResult.getInt(currentItem+2);	
					  } 
                   }
                 else
                   {
				   Routines.writeToLog(servletName,"Unable to find card (" + tempCardNumber + ")",false,context);	
                   }
                 }
               }
            }
       catch(SQLException error)
            {
			Routines.writeToLog(servletName,"Unable to retrieve actioncard : " + error,false,context);	
            }
      Routines.tableHeader("Amend details of Card",2,webPageOutput);
      }
      if("New Card".equals(action))
        {
        Routines.tableHeader("Enter details of new Card",2,webPageOutput);
        }
      boolean firstTime=true;
      for(int currentItem=0;currentItem<sizeOfCardArray-1;currentItem++)
         {
         if(firstTime)
           {
           }
         else
           { 
		   Routines.tableDataEnd(false,false,false,webPageOutput);	
           }
		 Routines.tableDataStart(true,false,false,true,false,5,0,"scoresrow",webPageOutput);
		 webPageOutput.print(cardItemText[currentItem]);
		 Routines.tableDataEnd(false,false,false,webPageOutput);
		 Routines.tableDataStart(true,false,false,false,false,30,0,"scoresrow",webPageOutput);
	     if(currentItem==1||currentItem==3)
		   {
		   int maxEntry=0;
		   if(currentItem==1)
		     {
		     maxEntry=12;		
		     }
		   if(currentItem==3)
		     {
		     maxEntry=48;	  
		     }
		    webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
			String selected="";
			for(int currentNumber=1;currentNumber<=maxEntry;currentNumber++)
			   {
			   if(currentNumber==cardData[currentItem]||(currentNumber==1&&"New Card".equals(action)))
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
		   }	
		 if(currentItem==0||currentItem==2||(currentItem>=4&&currentItem<=79))
		   {
		   boolean selected=false;
		   if(cardData[currentItem]==1||(currentItem==0&&"New Card".equals(action)))
		     {
		     selected=true;		
		     }
			webPageOutput.print("<INPUT TYPE=\"CHECKBOX\" NAME=\"" + currentItem  + "\" VALUE=\"true\"");
			if(selected)
			  {
			  webPageOutput.print(" CHECKED");
			  }
			webPageOutput.println(">");		   	
		   }
		if(currentItem==80)
		  {
		  webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
		   String selected="";
		   for(int currentNumber=0;currentNumber>=-6;currentNumber--)
			  {
			  if(currentNumber==cardData[currentItem]||(currentNumber==1&&"New Card".equals(action)))
				{
				selected=" SELECTED";
				}
			  else
				{
				selected="";
				}
			  String valueText="";
			  if(currentNumber<0)
			    {
			    valueText=String.valueOf(currentNumber);		
			    }
			  else
			    {
			    valueText="OK";	  
			    }
			  webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
			  }
		   webPageOutput.println("</SELECT>");		   	
		  }
		if(currentItem==81||currentItem==82||currentItem==83)
		  {
		  webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
		  String selected="";
		  for(int currentNumber=-1;currentNumber<=5;currentNumber++)
		     {
		     if(currentNumber==cardData[currentItem]||(currentNumber==0&&"New Card".equals(action)))
			   {
			   selected=" SELECTED";
			   }
			 else
			   {
			   selected="";
			   }
		     String valueText="";
		     switch(currentNumber)
				   {
			       case -1:
					  valueText="Pass Rush";
				      break;
				   case 0:
					  valueText="Original";
					  break;
				   case 1:
					  valueText="Left End";
					  break;
				   case 2:
					  valueText="Right End";
					  break;
				   case 3:
					  valueText="Back 1";
					  break;
				   case 4:
					  valueText="Back 2";
					  break;
				   case 5:
					  valueText="Flanker";
					  break;
				   default:
				      Routines.writeToLog(servletName,"PassingPlays value not found ("+currentNumber+")",false,context);	
				   }				  
				webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
				}
			 webPageOutput.println("</SELECT>");		   	
			}		  
		  if(currentItem==84)
			{
			webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
			String selected="";
			for(int currentNumber=-1;currentNumber<=4;currentNumber++)
			   {
			   if(currentNumber==cardData[currentItem]||(currentNumber==1&&"New Card".equals(action)))
				 {
				 selected=" SELECTED";
				 }
			   else
				 {
				 selected="";
				 }
			   String valueText="";
			   switch(currentNumber)
					 {
					 case -1:
						valueText="Intercepted";
						break;
					 case 0:
						valueText="Incomplete";
						break;
					 case 1:
						valueText="Complete";
						break;
					 case 2:
						valueText="Complete*2";
						break;
					 case 3:
						valueText="Complete*3";
						break;
					 case 4:
						valueText="Complete/2";
						break;
					 default:
						Routines.writeToLog(servletName,"Screen value not found ("+currentNumber+")",false,context);	
					 }				  
				  webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
				  }
			   webPageOutput.println("</SELECT>");		   	
			  }		 			
			if(currentItem==85)
			  {
			  webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
			  String selected="";
			  for(int currentNumber=-1;currentNumber<=26;currentNumber++)
				 {
				 if(currentNumber==cardData[currentItem]||(currentNumber==0&&"New Card".equals(action)))
				   {
				   selected=" SELECTED";
				   }
				 else
				   {
				   selected="";
				   }
				 String valueText="";
				 switch(currentNumber)
					   {
					   case -1:
						  valueText="Ball Carrier";
						  break;
					   case 0:
						  valueText="None";
						  break;
					   case 1:
						   valueText="Left End";
						   break;
					   case 2:
						  valueText="Left Tackle";
						  break;
					   case 3:
						  valueText="Left Guard";
						  break;
					   case 4:
						  valueText="Centre";
						  break;
					   case 5:
						  valueText="Right Guard";
						  break;
					   case 6:
						  valueText="Right Tackle";
						  break;
				       case 7:
						  valueText="Right End";
						  break;
					   case 8:
						  valueText="Back 1";
						  break;
					   case 9:
						  valueText="Back 2";
						  break;
					   case 10:
						  valueText="Back 3";
						  break;			
					   case 11:
					      valueText="QuarterBack";
					      break;
					   case 12:
					      valueText="Left Defensive End";
					      break;
					   case 13:
					      valueText="Left Defensive Tackle";
					      break;
					   case 14:
					      valueText="Nose Tackle";
					      break;
					   case 15:
					      valueText="Right Defenshive Tackle";
					      break;
					   case 16:
					      valueText="Right Defensive End";
					      break;
					   case 17:
					      valueText="Left Outside LineBacker";
					      break;
					   case 18:
					      valueText="Left Inside LineBacker";
					      break;
					   case 19:
					      valueText="Middle LineBacker";
					      break;
					   case 20:
					      valueText="Right Inside LineBacker";
					      break;
					   case 21:
					      valueText="Right Outside LineBacker";
					      break;
					   case 22:
					      valueText="Left Cornerback";
					      break;
					   case 23:
					      valueText="Nickel/Dime Back";
					      break;
					   case 24:
					      valueText="Free Safety";
					      break;
					   case 25:
					      valueText="Strong Safety";
					      break;
					   case 26:
					      valueText="Right Cornerback";
					      break;                                             	  			  
					   default:
						  Routines.writeToLog(servletName,"Injury value not found ("+currentNumber+")",false,context);	
					   }				  
					webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
					}
				 webPageOutput.println("</SELECT>");		   	
				}		 
			if(currentItem==86)
			  {
			  webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
			  String selected="";
			  for(int currentNumber=0;currentNumber<=2;currentNumber++)
				 {
				 if(currentNumber==cardData[currentItem]||(currentNumber==0&&"New Card".equals(action)))
				   {
				   selected=" SELECTED";
				   }
				 else
				   {
				   selected="";
				   }
				 String valueText="";
				 switch(currentNumber)
					   {
					   case 0:
						  valueText="None";
						  break;
					   case 1:
						  valueText="Fumble";
						  break;
					   case 2:
						  valueText="Fumble(s)";
						  break;
					   default:
						  Routines.writeToLog(servletName,"Fumble value not found ("+currentNumber+")",false,context);	
					   }				  
					webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
					}
				 webPageOutput.println("</SELECT>");		   	
				}	
			if(currentItem==87||currentItem==88||currentItem==89||currentItem==90)
			  {
			  webPageOutput.println("<SELECT NAME=\"" + currentItem + "\">");
			  String selected="";
			  for(int currentNumber=0;currentNumber<=2;currentNumber++)
				 {
				 if(currentNumber==cardData[currentItem]||(currentNumber==0&&"New Card".equals(action)))
				   {
				   selected=" SELECTED";
				   }
				 else
				   {
				   selected="";
				   }
				 String valueText="";
				 switch(currentNumber)
					   {
					   case 0:
						  valueText="None";
						  break;
					   case 1:
					      if(currentItem==87||currentItem==88)
					        {
					        valueText="Offense";
					        }
					      else
					        {
					        valueText="Kicking";	  
					        }
						  break;
					   case 2:
					      if(currentItem==87||currentItem==88)
						    {					   
						    valueText="Defense";
						    }
						  else
						    {
						    valueText="Returning";	  
						    }
						  break;
					   default:
						  Routines.writeToLog(servletName,"Penalty value not found ("+currentNumber+")",false,context);	
					   }				  
					webPageOutput.println(" <OPTION" + selected + " VALUE=\"" + currentNumber + "\">" + valueText);
					}
				 webPageOutput.println("</SELECT>");		   	
				}						 										  
        }  
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Actions",1,webPageOutput);
      Routines.tableDataStart(true,true,false,true,false,0,0,"scoresrow",webPageOutput);
      if("New Card".equals(action))
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store New Card\" NAME=\"action\">");
        }
      else
        {
        webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Store Changed Card\" NAME=\"action\">");
        }
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      Routines.tableDataEnd(false,false,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"cardNumber\" VALUE=\"" + cardNumber + "\">");
      webPageOutput.println("</FORM>");
      }

   private synchronized boolean updateEntry(String action,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            Connection database)
      {
      boolean success=false;
      int[] values=new int[sizeOfCardArray];
	  for(int currentItem=0;currentItem<sizeOfCardArray-1;currentItem++)
		 {
		 if(currentItem==0||currentItem==2||(currentItem>=4&&currentItem<=79))
		   {
		   String value=request.getParameter(String.valueOf(currentItem));
		   if("true".equals(value))
		     {
		     values[currentItem]=1;		
		     }
		   }
		 else
		   { 
		   values[currentItem]=Routines.safeParseInt(request.getParameter(String.valueOf(currentItem)));	 	
		   }
		 }	
	  if(values[0]==0)
	    {	 
	    for(int currentItem=1;currentItem<sizeOfCardArray-1;currentItem++)
		   {
		   values[currentItem]=0;	
		   }
        } 
      int cardNumber=Routines.safeParseInt(request.getParameter("cardNumber"));
      if(cardNumber==0)
        {
		try
		  {
		  Statement sql=database.createStatement();
		  ResultSet queryResult;
          //Get latest cardNumber.
          cardNumber=1;
          queryResult=sql.executeQuery("SELECT CardNumber " +
                                       "FROM actioncards " +
                                       "ORDER BY CardNumber DESC");
          if(queryResult.first())
            {
            cardNumber=queryResult.getInt(1) + 1;
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to retrieve actioncards : " + error,false,context);	
          }
        }  
      if("Store New Card".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          String sqlText=cardNumber+",";
		  for(int currentItem=0;currentItem<sizeOfCardArray-1;currentItem++)
			 {
			 sqlText+=values[currentItem]+",";	 	
			 }	
          updates=sql.executeUpdate("INSERT INTO actionCards (" +
		                            "CardNumber,Normal,RunNumber,OutOfBounds,PassNumber," +
		                            "SweepLeftBreak,SweepLeftLE,SweepLeftLT,SweepLeftLG,SweepLeftCN,SweepLeftRG,SweepLeftRT,SweepLeftRE," +
		                            "SweepLeftBK,SweepLeftLDE,SweepLeftLDT,SweepLeftNT,SweepLeftRDT,SweepLeftRDE,SweepLeftLOLB,SweepLeftLILB,SweepLeftMLB,SweepLeftRILB,SweepLeftROLB," +
		                            "InsideLeftBreak,InsideLeftLE,InsideLeftLT,InsideLeftLG,InsideLeftCN,InsideLeftRG,InsideLeftRT,InsideLeftRE," +
		                            "InsideLeftBK,InsideLeftLDE,InsideLeftLDT,InsideLeftNT,InsideLeftRDT,InsideLeftRDE,InsideLeftLOLB,InsideLeftLILB,InsideLeftMLB,InsideLeftRILB,InsideLeftROLB," +
		                            "SweepRightBreak,SweepRightLE,SweepRightLT,SweepRightLG,SweepRightCN,SweepRightRG,SweepRightRT,SweepRightRE," +
		                            "SweepRightBK,SweepRightLDE,SweepRightLDT,SweepRightNT,SweepRightRDT,SweepRightRDE,SweepRightLOLB,SweepRightLILB,SweepRightMLB,SweepRightRILB,SweepRightROLB," +
		                            "InsideRightBreak,InsideRightLE,InsideRightLT,InsideRightLG,InsideRightCN,InsideRightRG,InsideRightRT,InsideRightRE," +
		                            "InsideRightBK,InsideRightLDE,InsideRightLDT,InsideRightNT,InsideRightRDT,InsideRightRDE,InsideRightLOLB,InsideRightLILB,InsideRightMLB,InsideRightRILB,InsideRightROLB," +
		                            "EndAround,QuickPass,ShortPass,LongPass,Screen,Injury,Fumble,RunQuickPenalty,ShortLongPenalty,PuntPenalty,KickOffPenalty,DateTimeStamp) " +
                                    "VALUES (" +
                                    sqlText + "'" +
                                    Routines.getDateTime(false) + "')");
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"New card not created, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to create card : " + error,false,context);
          }
        session.setAttribute("message",cardNumber + " card stored successfully");
        success=true;
        }
      if("Store Changed Card".equals(action))
        {
        try
          {
          int updates=0;
          Statement sql=database.createStatement();
          ResultSet queryResult;
          updates=sql.executeUpdate("UPDATE actioncards " +
                                    "SET " +
			                        "CardNumber=" + cardNumber + "," +
			                        "Normal=" + values[0] + "," +
			                        "RunNumber=" + values[1] + "," +
			                        "OutOfBounds=" + values[2] + "," +
			                        "PassNumber=" + values[3] + "," +
			                        "SweepLeftBreak=" + values[4] + "," +
			                        "SweepLeftLE=" + values[5] + "," +
			                        "SweepLeftLT=" + values[6] + "," +
			                        "SweepLeftLG=" + values[7] + "," +
			                        "SweepLeftCN=" + values[8] + "," +
			                        "SweepLeftRG=" + values[9] + "," +
			                        "SweepLeftRT=" + values[10] + "," +
                                    "SweepLeftRE=" + values[11] + "," +
			                        "SweepLeftBK=" + values[12] + "," +
			                        "SweepLeftLDE=" + values[13] + "," +
			                        "SweepLeftLDT=" + values[14] + "," +
			                        "SweepLeftNT=" + values[15] + "," +
			                        "SweepLeftRDT=" + values[16] + "," +
			                        "SweepLeftRDE=" + values[17] + "," +
			                        "SweepLeftLOLB=" + values[18] + "," +
			                        "SweepLeftLILB=" + values[19] + "," +
			                        "SweepLeftMLB=" + values[20] + "," +
			                        "SweepLeftRILB=" + values[21] + "," +
			                        "SweepLeftROLB=" + values[22] + "," +
			                        "InsideLeftBreak=" + values[23] + "," +
			                        "InsideLeftLE=" + values[24] + "," +
			                        "InsideLeftLT=" + values[25] + "," +
			                        "InsideLeftLG=" + values[26] + "," +
			                        "InsideLeftCN=" + values[27] + "," +
			                        "InsideLeftRG=" + values[28] + "," +
			                        "InsideLeftRT=" + values[29] + "," +
			                        "InsideLeftRE=" + values[30] + "," +
			                        "InsideLeftBK=" + values[31] + "," +
			                        "InsideLeftLDE=" + values[32] + "," +
			                        "InsideLeftLDT=" + values[33] + "," +
			                        "InsideLeftNT=" + values[34] + "," +
			                        "InsideLeftRDT=" + values[35] + "," +
			                        "InsideLeftRDE=" + values[36] + "," +
			                        "InsideLeftLOLB=" + values[37] + "," +
			                        "InsideLeftLILB=" + values[38] + "," +
			                        "InsideLeftMLB=" + values[39] + "," +
			                        "InsideLeftLILB=" + values[40] + "," +
			                        "InsideLeftLOLB=" + values[41] + "," +
			                        "SweepRightBreak=" + values[42] + "," +
			                        "SweepRightLE=" + values[43] + "," +
			                        "SweepRightLT=" + values[44] + "," +
			                        "SweepRightLG=" + values[45] + "," +
			                        "SweepRightCN=" + values[46] + "," +
			                        "SweepRightRG=" + values[47] + "," +
			                        "SweepRightRT=" + values[48] + "," +
			                        "SweepRightRE=" + values[49] + "," +
			                        "SweepRightBK=" + values[50] + "," +
			                        "SweepRightLDE=" + values[51] + "," +
			                        "SweepRightLDT=" + values[52] + "," +
			                        "SweepRightNT=" + values[53] + "," +
			                        "SweepRightRDT=" + values[54] + "," +
			                        "SweepRightRDE=" + values[55] + "," +
			                        "SweepRightLOLB=" + values[56] + "," +
			                        "SweepRightLILB=" + values[57] + "," +
			                        "SweepRightMLB=" + values[58] + "," +
			                        "SweepRightRILB=" + values[59] + "," +
			                        "SweepRightROLB=" + values[60] + "," +
			                        "InsideRightBreak=" + values[61] + "," +
			                        "InsideRightLE=" + values[62] + "," +
			                        "InsideRightLT=" + values[63] + "," +
			                        "InsideRightLG=" + values[64] + "," +
			                        "InsideRightCN=" + values[65] + "," +
			                        "InsideRightRG=" + values[66] + "," +
			                        "InsideRightRT=" + values[67] + "," +
			                        "InsideRightRE=" + values[68] + "," +
			                        "InsideRightBK=" + values[69] + "," +
			                        "InsideRightLDE=" + values[70] + "," +
			                        "InsideRightLDT=" + values[71] + "," +
			                        "InsideRightNT=" + values[72] + "," +
			                        "InsideRightRDT=" + values[73] + "," +
			                        "InsideRightRDE=" + values[74] + "," +
			                        "InsideRightLOLB=" + values[75] + "," +
			                        "InsideRightLILB=" + values[76] + "," +
			                        "InsideRightMLB=" + values[77] + "," +
			                        "InsideRightLILB=" + values[78] + "," +
			                        "InsideRightLOLB=" + values[79] + "," +
			                        "EndAround=" + values[80] + "," +
			                        "QuickPass=" + values[81] + "," +
			                        "ShortPass=" + values[82] + "," +
			                        "LongPass=" + values[83] + "," +
			                        "Screen=" + values[84] + "," +
			                        "Injury=" + values[85] + "," +
			                        "Fumble=" + values[86] + "," +
			                        "RunQuickPenalty=" + values[87] + "," +
			                        "ShortLongPenalty=" + values[88] + "," +
			                        "PuntPenalty=" + values[89] + "," +
			                        "KickOffPenalty=" + values[90] + "," + 
                                    "DateTimeStamp='" +
                                    Routines.getDateTime(false) + "' " +
                                    "WHERE CardNumber=" + cardNumber);
          if(updates!=1)
            {
			Routines.writeToLog(servletName,"Card not updated, reason unknown",false,context);	
            }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to update actioncards : " + error,false,context);	
          }
        session.setAttribute("message",cardNumber + " card changed successfully");
        success=true;
        }
      if("Delete Card".equals(action))
        {
        boolean deleteRequested=false;
        try
          {
          int updates=0;
          Statement sql1=database.createStatement();
          Statement sql2=database.createStatement();
          ResultSet queryResult1;
          ResultSet queryResult2;
          queryResult1=sql1.executeQuery("SELECT CardNumber " +
                                         "FROM actioncards");
          while(queryResult1.next())
               {
               cardNumber=queryResult1.getInt(1);
               if("true".equals(request.getParameter(String.valueOf(cardNumber))))
                 {
                 if(!deleteRequested)
                   {
                   deleteRequested=true;
                   }
                 updates=sql2.executeUpdate("DELETE FROM actioncards " +
                                            "WHERE CardNumber=" + cardNumber);
                 if(updates!=1)
                   {
				   Routines.writeToLog(servletName,"card not deleted (" + cardNumber + ")",false,context);	
                   }
                 }
               }
          }
        catch(SQLException error)
          {
		  Routines.writeToLog(servletName,"Unable to delete actioncards : " + error,false,context);	
          }
        if(deleteRequested)
          {
          session.setAttribute("message","Delete successfull");
          }
        else
          {
          session.setAttribute("message","No card selected");
          }
        success=true;
        }
      return success;
      }
}