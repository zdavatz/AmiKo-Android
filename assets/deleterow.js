function deleterow(tableID,currentRow) {
    try {
		if (tableID=="Notify_interaction") {
			jsInterface.sendMessage("notify_interaction")
		} else if (tableID=="Delete_all") {
			jsInterface.sendMessage("delete_all");
		} else {
			var table = document.getElementById(tableID);
			var rowCount = table.rows.length;
			for (var i=0; i<rowCount; i++) {
				var row = table.rows[i];
				if (row==currentRow.parentNode.parentNode) {
                    jsInterface.sendMessage(row.cells[1].innerText);
					table.deleteRow(i);
					rowCount--;
				}
			}		
        }
    } catch (e) {
        window.alert(e);
    }
}
