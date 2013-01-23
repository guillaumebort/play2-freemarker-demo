<#macro main title>
	
	<html>
	    <head>
	        <title>${title}</title>
	        <link rel="stylesheet" media="screen" href="${Router.Assets.at("stylesheets/main.css")}">
	        <link rel="shortcut icon" type="image/png" href="${Router.Assets.at("images/favicon.png")}">
	        <script src="{Router.Assets.at("javascripts/jquery-1.7.1.min.js")}" type="text/javascript"></script>
	    </head>
	    <body>
	    	<h1>Hello ${user ! 'Guest'}, this page is rendered with Freemarker</h1>

	        <#nested/>

	        <hr>

	        <#if user??>
	        	You are connected as ${user} – <a href="${Router.Application.logout()}">Logout</a>
	        <#else>
	        	<form action="${Router.Application.login()}" method="POST">
	        		<input type="text" name="user" placeholder="Your login">
	        		<input type="submit" value="Sign in">
	        	</form>
	        </#if>  

	    </body>
	</html>

</#macro>