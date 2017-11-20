/**
 * 
 */

function noojeeApproveTicket(ticketId, activity_ids)
{
	try
	{
		
			// Approve activities
			var urlApproveActivities = "https://noojee.accelo.com/api/0.5/key/activity/list";
			
			xmlhttp=new XMLHttpRequest();
			var formData = new FormData();

			// formData.append(activity_ids + "&approve=1")

			// var photoId = getCookie("user");

			xmlhttp.onreadystatechange=function()
			{
			    alert("xhr status : "+xmlhttp.readyState);
			}


			xmlhttp.open("GET", urlApproveActivities);

			xmlhttp.setRequestHeader("X-API-Key", "03341ef66bc8ea0a28f07ca4f8e74c101f7001aa"); // Auth key
			
			xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

			xmlhttp.send(activity_ids + "&approve=1");

			
			// Now approve the ticket
			
			// https://noojee.accelo.com/api/0.5/key/issue/10683/approval
			// Referer:https://noojee.accelo.com/?action=approve_object&object_id=10683&object_table=issue&amp;referring_link=action%3Dview_issue%26id%3D10683
			// X-API-Key:03341ef66bc8ea0a28f07ca4f8e74c101f7001aa
			
			
			
	}
	catch (e)
	{ 
		alert("Error Occured: " + e);
	}
	
}