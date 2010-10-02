import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

public class Main extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Main";

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
      session.setAttribute("redirect",request.getRequestURL() + "?action=viewMain");
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead(null,//title
                             false,//showMenu
                             0,//menuHighLight
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
      session.setAttribute("league",String.valueOf(0));
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<OBJECT classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\"");
      webPageOutput.println("codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=5,0,0,0\"");
      webPageOutput.println(" WIDTH=468 HEIGHT=60>");
      webPageOutput.println("<PARAM NAME=movie VALUE=\"../Images/clubpit-468X60.swf\">");
      webPageOutput.println("<PARAM NAME=quality VALUE=high>");
      webPageOutput.println("<PARAM NAME=bgcolor VALUE=#FFFFFF>");
      webPageOutput.println("<EMBED src=\"../Images/clubpit-468X60.swf\" quality=high bgcolor=#FFFFFF  WIDTH=468 HEIGHT=60 TYPE=\"application/x-shockwave-flash\" PLUGINSPAGE=\"http://www.macromedia.com/shockwave/download/index.cgi?P1_Prod_Version=ShockwaveFlash\"></EMBED>");
      webPageOutput.println("</OBJECT>");
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<IMG SRC=\"../Images/Football.jpg\"" +
                            " WIDTH='549' HEIGHT='78' ALT='Football'>");
      webPageOutput.println("</CENTER>");
	  Calendar currentCalendar=Calendar.getInstance();
	  int day=currentCalendar.get(Calendar.DAY_OF_MONTH);
      int month=currentCalendar.get(Calendar.MONTH)+1;
      int year=currentCalendar.get(Calendar.YEAR);
	  boolean floatingImage=false;
	  String floatingFile="";
	  floatingImage=true;
	  if(month==02&&day==14)
		{
		floatingFile="\"../Images/Valentines.gif\"";	
		}	  
	  if(month==12&&day<13)
	    {
		floatingFile="\"../Images/SnowFlake.gif\"";	
	    }
	  if(month==12&&day>12)
		{
		floatingFile="\"../Images/Santa.gif\"";	
		}
	  if(month==01&&day==01&&year==2006)
		{
		floatingFile="\"../Images/2006.gif\"";	
		}
	  if(month==01&&day==01&&year==2007)
		{
		floatingFile="\"../Images/2007.gif\"";	
		}
	  if(month==01&&day==01&&year==2008)
		{
		floatingFile="\"../Images/2008.gif\"";	
		}
	  if(month==01&&day==01&&year==2009)
		{
		floatingFile="\"../Images/2009.gif\"";	
		}
	  if(month==01&&day==01&&year==2010)
		{
		floatingFile="\"../Images/2010.gif\"";	
		}			    
	  
	  if(floatingImage)
	    {
	    webPageOutput.println("<script language=\"JavaScript1.2\">");
	    webPageOutput.println("/******************************************");
	    webPageOutput.println("* Snow Effect Script- By Altan d.o.o. (snow@altan.hr, http://www.altan.hr/snow/index.html)");
	    webPageOutput.println("* Visit Dynamic Drive (http://www.dynamicdrive.com/) for full source code");
	    webPageOutput.println("* Modified Dec 31st, 02' by DD. This notice must stay intact for use");
	    webPageOutput.println("******************************************/");
	    webPageOutput.println("//Configure below to change URL path to the snow image");
	    webPageOutput.println("var snowsrc="+floatingFile);
	    webPageOutput.println("// Configure below to change number of snow to render");
	    webPageOutput.println("var no = 10;");
	    webPageOutput.println("var ns4up = (document.layers) ? 1 : 0;  // browser sniffer");
	    webPageOutput.println("var ie4up = (document.all) ? 1 : 0;");
	    webPageOutput.println("var ns6up = (document.getElementById&&!document.all) ? 1 : 0;");
	    webPageOutput.println("var dx, xp, yp;    // coordinate and position variables");
	    webPageOutput.println("var am, stx, sty;  // amplitude and step variables");
	    webPageOutput.println("var i, doc_width = 800, doc_height = 600;");
	    webPageOutput.println("if (ns4up||ns6up) {");
	    webPageOutput.println("doc_width = self.innerWidth;");
	    webPageOutput.println("doc_height = self.innerHeight;");
	    webPageOutput.println("} else if (ie4up) {");
	    webPageOutput.println("doc_width = document.body.clientWidth;");
	    webPageOutput.println("doc_height = document.body.clientHeight;");
	    webPageOutput.println("}");
	    webPageOutput.println("dx = new Array();");
	    webPageOutput.println("xp = new Array();");
	    webPageOutput.println("yp = new Array();");
	    webPageOutput.println("am = new Array();");
	    webPageOutput.println("stx = new Array();");
	    webPageOutput.println("sty = new Array();");
	    webPageOutput.println("for (i = 0; i < no; ++ i) {");  
	    webPageOutput.println("dx[i] = 0;                        // set coordinate variables");
	    webPageOutput.println("xp[i] = Math.random()*(doc_width-50);  // set position variables");
	    webPageOutput.println("yp[i] = Math.random()*doc_height;");
	    webPageOutput.println("am[i] = Math.random()*20;         // set amplitude variables");
	    webPageOutput.println("stx[i] = 0.02 + Math.random()/10; // set step variables");
	    webPageOutput.println("sty[i] = 0.7 + Math.random();     // set step variables");
	    webPageOutput.println("if (ns4up) {                      // set layers");
	    webPageOutput.println("if (i == 0) {");
	    webPageOutput.println("document.write(\"<layer name=\\\"dot\"+ i +\"\\\" left=\\\"15\\\" top=\\\"15\\\" visibility=\\\"show\\\"><a href=\\\"http://dynamicdrive.com/\\\"><img src='\"+snowsrc+\"' border=\\\"0\\\"><\\/a></layer>\");");
	    webPageOutput.println("} else {");
	    webPageOutput.println("document.write(\"<layer name=\\\"dot\"+ i +\"\\\" left=\\\"15\\\" top=\\\"15\\\" visibility=\\\"show\\\"><img src='\"+snowsrc+\"' border=\\\"0\\\"><\\/layer>\");");
	    webPageOutput.println("}");
	    webPageOutput.println("} else if (ie4up||ns6up) {");
	    webPageOutput.println("if (i == 0) {");
	    webPageOutput.println("document.write(\"<div id=\\\"dot\"+ i +\"\\\" style=\\\"POSITION: absolute; Z-INDEX: \"+ i +\"; VISIBILITY: visible; TOP: 15px; LEFT: 15px;\\\"><a href=\\\"http://dynamicdrive.com\\\"><img src='\"+snowsrc+\"' border=\\\"0\\\"><\\/a><\\/div>\");");
	    webPageOutput.println("} else {");
	    webPageOutput.println("document.write(\"<div id=\\\"dot\"+ i +\"\\\" style=\\\"POSITION: absolute; Z-INDEX: \"+ i +\"; VISIBILITY: visible; TOP: 15px; LEFT: 15px;\\\"><img src='\"+snowsrc+\"' border=\\\"0\\\"><\\/div>\");");
	    webPageOutput.println("}");
	    webPageOutput.println("}");
	    webPageOutput.println("}");
	    webPageOutput.println("function snowNS() {  // Netscape main animation function");
	    webPageOutput.println("for (i = 0; i < no; ++ i) {  // iterate for every dot");
	    webPageOutput.println("yp[i] += sty[i];");
	    webPageOutput.println("if (yp[i] > doc_height-50) {");
	    webPageOutput.println("xp[i] = Math.random()*(doc_width-am[i]-30);");
	    webPageOutput.println("yp[i] = 0;");
	    webPageOutput.println("stx[i] = 0.02 + Math.random()/10;");
	    webPageOutput.println("sty[i] = 0.7 + Math.random();");
	    webPageOutput.println("doc_width = self.innerWidth;");
	    webPageOutput.println("doc_height = self.innerHeight;");
	    webPageOutput.println("}");
	    webPageOutput.println("dx[i] += stx[i];");
	    webPageOutput.println("document.layers[\"dot\"+i].top = yp[i];");
	    webPageOutput.println("document.layers[\"dot\"+i].left = xp[i] + am[i]*Math.sin(dx[i]);");
	    webPageOutput.println("}");
	    webPageOutput.println("setTimeout(\"snowNS()\", 10);");
	    webPageOutput.println("}");
	    webPageOutput.println("function snowIE_NS6() {  // IE and NS6 main animation function");
	    webPageOutput.println("for (i = 0; i < no; ++ i) {  // iterate for every dot");
	    webPageOutput.println("yp[i] += sty[i];");
	    webPageOutput.println("if (yp[i] > doc_height-50) {");
	    webPageOutput.println("xp[i] = Math.random()*(doc_width-am[i]-30);");
	    webPageOutput.println("yp[i] = 0;");
	    webPageOutput.println("stx[i] = 0.02 + Math.random()/10;");
	    webPageOutput.println("sty[i] = 0.7 + Math.random();");
	    webPageOutput.println("doc_width = ns6up?window.innerWidth : document.body.clientWidth;");
	    webPageOutput.println("doc_height = ns6up?window.innerHeight : document.body.clientHeight;");
	    webPageOutput.println("}");
	    webPageOutput.println("dx[i] += stx[i];");
	    webPageOutput.println("if (ie4up){");
	    webPageOutput.println("document.all[\"dot\"+i].style.pixelTop = yp[i];");
	    webPageOutput.println("document.all[\"dot\"+i].style.pixelLeft = xp[i] + am[i]*Math.sin(dx[i]);");
	    webPageOutput.println("}");
	    webPageOutput.println("else if (ns6up){");
	    webPageOutput.println("document.getElementById(\"dot\"+i).style.top=yp[i];");
	    webPageOutput.println("document.getElementById(\"dot\"+i).style.left=xp[i] + am[i]*Math.sin(dx[i]);");
	    webPageOutput.println("}");
	    webPageOutput.println("}");
	    webPageOutput.println("setTimeout(\"snowIE_NS6()\", 10);");
	    webPageOutput.println("}");
	    webPageOutput.println("if (ns4up) {");
	    webPageOutput.println("snowNS();");
	    webPageOutput.println("} else if (ie4up||ns6up) {");
	    webPageOutput.println("snowIE_NS6();");
	    webPageOutput.println("}");
	    webPageOutput.println("</script>");
	    }
      // Launch Countdown
      java.util.Date targetDate;
      java.util.Date currentDate;
      long targetDateMil=0;
      long currentDateMil=0;
      boolean showCountDown=false;
      Calendar targetCalendar=Calendar.getInstance();
      targetCalendar.set(Calendar.YEAR,2006);
      targetCalendar.set(Calendar.MONTH,Calendar.JANUARY);
      targetCalendar.set(Calendar.DATE,1);
      targetCalendar.set(Calendar.HOUR_OF_DAY,20);
      targetCalendar.set(Calendar.MINUTE,0);
      targetCalendar.set(Calendar.SECOND,0);
      targetDate=targetCalendar.getTime();
      currentDate=currentCalendar.getTime();
      targetDateMil=targetDate.getTime();
      currentDateMil=currentDate.getTime();
      if(currentDateMil<targetDateMil)
        {
        showCountDown=true;
        }
      if(showCountDown)
        {
        webPageOutput.println("<CENTER>");
        Routines.tableStart(true,webPageOutput);
        Routines.tableHeader("Countdown To Launch",1,webPageOutput);
        Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
		webPageOutput.println("<FONT COLOR=\"#FF0000\">");
        Routines.countDown(targetCalendar,webPageOutput);
		webPageOutput.println("</FONT>");
        Routines.tableDataEnd(true,false,true,webPageOutput);
        Routines.tableEnd(webPageOutput);
        webPageOutput.println("</CENTER>");
        }
      // End of Launch Countdown
      webPageOutput.println("<CENTER>");
      if(showCountDown)
        {
        webPageOutput.println("<IMG SRC=\"../Images/Launch.gif\"" +
                              " WIDTH='40' HEIGHT='110' ALT='Rocket Launch' ALIGN=\"TOP\">");
        }
      webPageOutput.println("<IMG SRC=\"../Images/FootballSpinning.gif\"" +
                            " WIDTH='260' HEIGHT='260' ALT='ClubPit OpenFootball'>");
      if(showCountDown)
        {
        webPageOutput.println("<IMG SRC=\"../Images/Launch.gif\"" +
                              " WIDTH='40' HEIGHT='110' ALT='Rocket Launch' ALIGN=\"TOP\">");
        }
      webPageOutput.println("</CENTER>");
      webPageOutput.println(Routines.spaceLines(2));
      try
       {
       String minorVersionText="";
       int majorVersion=0;
       int minorVersion=0;
       java.sql.Date loadedDate= new java.sql.Date(0);
       Statement sql=database.createStatement();
       ResultSet queryResults=sql.executeQuery("SELECT MajorVersion,MinorVersion,ChangeDate " +
                                               "FROM changelog " +
                                               "ORDER BY ChangeLog DESC");
       if(queryResults.first())
	 {
         majorVersion=queryResults.getInt(1);
         minorVersion=queryResults.getInt(2);
         loadedDate=queryResults.getDate(3);
         minorVersionText=Routines.minorVersionText(minorVersion);
         webPageOutput.println("<CENTER>");
         Routines.WriteHTMLLink(request,
                                response,
                                webPageOutput,
                                "VersionLog",
                                "majorVersion=" +
                                majorVersion +
                                "&minorVersion=" +
                                minorVersion,
                                "Version " +
                                majorVersion +
                                "." +
                                minorVersionText +
                                " loaded " +
                                Routines.reformatDate(loadedDate),
                                "whiteLink",
                                true);
         webPageOutput.println("</CENTER>");
         }
       }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Unable to access ChangeLog : " + error,false,context);	
         }
      webPageOutput.println(Routines.spaceLines(2));
      Routines.WriteHTMLTail(request,response,webPageOutput);
	  pool.returnConnection(database);
	  database=null;
      }
   }