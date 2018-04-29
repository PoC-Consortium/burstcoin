/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.loadContacts = function() {
	BRS.contacts = {};

	BRS.database.select("contacts", null, function(error, contacts) {
	    if (contacts && contacts.length) {
		$.each(contacts, function(index, contact) {
		    BRS.contacts[contact.accountRS] = contact;
		});
	    }
	});
    }

    BRS.pages.contacts = function() {
	if (!BRS.databaseSupport) {
	    $("#contact_page_database_error").show();
	    $("#contacts_table_container").hide();
	    $("#add_contact_button").hide();
	    BRS.pageLoaded();
	    return;
	}

	$("#contacts_table_container").show();
	$("#contact_page_database_error").hide();

	BRS.database.select("contacts", null, function(error, contacts) {
	    var rows = "";

	    if (contacts && contacts.length) {
		contacts.sort(function(a, b) {
		    if (a.name.toLowerCase() > b.name.toLowerCase()) {
			return 1;
		    }
                    else if (a.name.toLowerCase() < b.name.toLowerCase()) {
			return -1;
		    }
                    else {
			return 0;
		    }
		});

		$.each(contacts, function(index, contact) {
		    var contactDescription = contact.description;

		    if (contactDescription.length > 100) {
			contactDescription = contactDescription.substring(0, 100) + "...";
		    }
                    else if (!contactDescription) {
			contactDescription = "-";
		    }

		    rows += "<tr><td><a href='#' data-toggle='modal' data-target='#update_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>" + contact.name.escapeHTML() + "</a></td><td><a href='#' data-user='" + BRS.getAccountFormatted(contact, "account") + "' class='user_info'>" + BRS.getAccountFormatted(contact, "account") + "</a></td><td>" + (contact.email ? contact.email.escapeHTML() : "-") + "</td><td>" + contactDescription.escapeHTML() + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_money_modal' data-contact='" + String(contact.name).escapeHTML() + "'>" + $.t("send_burst") + "</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_message_modal' data-contact='" + String(contact.name).escapeHTML() + "'>" + $.t("message") + "</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#delete_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>" + $.t("delete") + "</a></td></tr>";
		});
	    }

	    BRS.dataLoaded(rows);
	});
    }

    BRS.forms.addContact = function($modal) {
	var data = BRS.getFormData($modal.find("form:first"));

	data.account_id = String(data.account_id);

	if (!data.name) {
	    return {
		"error": $.t("error_contact_name_required")
	    };
	}
        else if (!data.account_id) {
	    return {
		"error": $.t("error_account_id_required")
	    };
	}

	if (/^\d+$/.test(data.name) || /^BURST\-/i.test(data.name)) {
	    return {
		"error": $.t("error_contact_name_alpha")
	    };
	}

	if (data.email && !/@/.test(data.email)) {
	    return {
		"error": $.t("error_email_address")
	    };
	}

	if (data.account_id.charAt(0) == '@') {
	    var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
	    if (convertedAccountId) {
		data.account_id = convertedAccountId;
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}

	if (/^BURST\-/i.test(data.account_id)) {
	    data.account_rs = data.account_id;

	    var address = new NxtAddress();

	    if (address.set(data.account_rs)) {
		data.account = address.account_id();
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}
        else {
	    var address = new NxtAddress();

	    if (address.set(data.account_id)) {
		data.account_rs = address.toString();
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}

	BRS.sendRequest("getAccount", {
	    "account": data.account_id
	}, function(response) {
	    if (!response.errorCode) {
		if (response.account != data.account || response.accountRS != data.account_rs) {
		    return {
			"error": $.t("error_account_id")
		    };
		}
	    }
	}, false);

	var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

	BRS.database.select("contacts", [{
	    "account": data.account_id
	}, {
	    "name": data.name
	}], function(error, contacts) {
	    if (contacts && contacts.length) {
		if (contacts[0].name == data.name) {
		    $modal.find(".error_message").html($.t("error_contact_name_exists")).show();
		}
                else {
		    $modal.find(".error_message").html($.t("error_contact_account_id_exists")).show();
		}
		$btn.button("reset");
		$modal.modal("unlock");
	    }
            else {
		BRS.database.insert("contacts", {
		    name: data.name,
		    email: data.email,
		    account: data.account_id,
		    accountRS: data.account_rs,
		    description: data.description
		}, function(error) {
		    BRS.contacts[data.account_id] = {
			name: data.name,
			email: data.email,
			account: data.account_id,
			accountRS: data.account_rs,
			description: data.description
		    };

		    setTimeout(function() {
			$btn.button("reset");
			$modal.modal("unlock");
			$modal.modal("hide");
			$.notify($.t("success_contact_add"), {
			    "type": "success"
			});

			if (BRS.currentPage == "contacts") {
			    BRS.loadPage("contacts");
			}
                        else if (BRS.currentPage == "messages" && BRS.selectedContext) {
			    var heading = BRS.selectedContext.find("h4.list-group-item-heading");
			    if (heading.length) {
				heading.html(data.name.escapeHTML());
			    }
			    BRS.selectedContext.data("context", "messages_sidebar_update_context");
			}
		    }, 50);
		});
	    }
	});
    }

    $("#update_contact_modal").on("show.bs.modal", function(e) {
	var $invoker = $(e.relatedTarget);

	var contactId = parseInt($invoker.data("contact"), 10);

	if (!contactId && BRS.selectedContext) {
	    var accountId = BRS.selectedContext.data("account");

	    var dbKey = (/^BURST\-/i.test(accountId) ? "accountRS" : "account");

	    var dbQuery = {};
	    dbQuery[dbKey] = accountId;

	    BRS.database.select("contacts", [dbQuery], function(error, contact) {
		contact = contact[0];

		$("#update_contact_id").val(contact.id);
		$("#update_contact_name").val(contact.name);
		$("#update_contact_email").val(contact.email);
		$("#update_contact_account_id").val(contact.accountRS);
		$("#update_contact_description").val(contact.description);
	    });
	}
        else {
	    $("#update_contact_id").val(contactId);

	    BRS.database.select("contacts", [{
		"id": contactId
	    }], function(error, contact) {
		contact = contact[0];

		$("#update_contact_name").val(contact.name);
		$("#update_contact_email").val(contact.email);
		$("#update_contact_account_id").val(contact.accountRS);
		$("#update_contact_description").val(contact.description);
	    });
	}
    });

    BRS.forms.updateContact = function($modal) {
	var data = BRS.getFormData($modal.find("form:first"));

	data.account_id = String(data.account_id);

	if (!data.name) {
	    return {
		"error": $.t("error_contact_name_required")
	    };
	}
        else if (!data.account_id) {
	    return {
		"error": $.t("error_account_id_required")
	    };
	}

	if (data.account_id.charAt(0) == '@') {
	    var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
	    if (convertedAccountId) {
		data.account_id = convertedAccountId;
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}

	var contactId = parseInt($("#update_contact_id").val(), 10);

	if (!contactId) {
	    return {
		"error": $.t("error_contact")
	    };
	}

	if (/^BURST\-/i.test(data.account_id)) {
	    data.account_rs = data.account_id;

	    var address = new NxtAddress();

	    if (address.set(data.account_rs)) {
		data.account_id = address.account_id();
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}
        else {
	    var address = new NxtAddress();

	    if (address.set(data.account_id)) {
		data.account_rs = address.toString();
	    }
            else {
		return {
		    "error": $.t("error_account_id")
		};
	    }
	}

	BRS.sendRequest("getAccount", {
	    "account": data.account_id
	}, function(response) {
	    if (!response.errorCode) {
		if (response.account != data.account_id || response.accountRS != data.account_rs) {
		    return {
			"error": $.t("error_account_id")
		    };
		}
	    }
	}, false);

	var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");

	BRS.database.select("contacts", [{
	    "account": data.account_id
	}], function(error, contacts) {
	    if (contacts && contacts.length && contacts[0].id != contactId) {
		$modal.find(".error_message").html($.t("error_contact_exists")).show();
		$btn.button("reset");
		$modal.modal("unlock");
	    }
            else {
		BRS.database.update("contacts", {
		    name: data.name,
		    email: data.email,
		    account: data.account_id,
		    accountRS: data.account_rs,
		    description: data.description
		}, [{
		    "id": contactId
		}], function(error) {
		    if (contacts.length && data.account_id != contacts[0].accountId) {
			delete BRS.contacts[contacts[0].accountId];
		    }

		    BRS.contacts[data.account_id] = {
			name: data.name,
			email: data.email,
			account: data.account_id,
			accountRS: data.account_rs,
			description: data.description
		    };

		    setTimeout(function() {
			$btn.button("reset");
			$modal.modal("unlock");
			$modal.modal("hide");
			$.notify($.t("success_contact_update"), {
			    "type": "success"
			});

			if (BRS.currentPage == "contacts") {
			    BRS.loadPage("contacts");
			}
                        else if (BRS.currentPage == "messages" && BRS.selectedContext) {
			    var heading = BRS.selectedContext.find("h4.list-group-item-heading");
			    if (heading.length) {
				heading.html(data.name.escapeHTML());
			    }
			}
		    }, 50);
		});
	    }
	});
    }

    $("#delete_contact_modal").on("show.bs.modal", function(e) {
	var $invoker = $(e.relatedTarget);

	var contactId = $invoker.data("contact");

	$("#delete_contact_id").val(contactId);

	BRS.database.select("contacts", [{
	    "id": contactId
	}], function(error, contact) {
	    contact = contact[0];

	    $("#delete_contact_name").html(contact.name.escapeHTML());
	    $("#delete_contact_account_id").val(BRS.getAccountFormatted(contact, "account"));
	});
    });

    BRS.forms.deleteContact = function($modal) {
	var id = parseInt($("#delete_contact_id").val(), 10);

	BRS.database.delete("contacts", [{
	    "id": id
	}], function() {
	    delete BRS.contacts[$("#delete_contact_account_id").val()];

	    setTimeout(function() {
		$.notify($.t("success_contact_delete"), {
		    "type": "success"
		});

		if (BRS.currentPage == "contacts") {
		    BRS.loadPage("contacts");
		}
	    }, 50);
	});

	return {
	    "stop": true
	};
    }
    BRS.exportContacts = function() {
	if (BRS.contacts && (Object.keys(BRS.contacts).length > 0)) {
	    var contacts_download = document.createElement('a');
	    contacts_download.href = 'data:attachment/json,' + encodeURIComponent(JSON.stringify( BRS.contacts ));
	    contacts_download.target = '_blank';
	    contacts_download.download = 'contacts.json';
	    document.body.appendChild(contacts_download);
	    contacts_download.click();
	    document.body.removeChild(contacts_download);
	}
        else {
	    console.log('No contacts found in database to backup');
	}
    }
    $("#export_contacts_button").on("click", function() {
	BRS.exportContacts();
    });
    BRS.importContacts = function(imported_contacts) {
	console.log('Import contacts called');
	console.log(imported_contacts);

	$.each(imported_contacts, function(index, imported_contact) {
	    console.log('Importing contact ' + imported_contact.name);
	    
	    BRS.database.select("contacts", [{
		"account": imported_contact.account
	    }, {
		"name": imported_contact.name
	    }], function(error, contacts) {
		if (contacts && contacts.length) {
		    if (contacts[0].name == imported_contact.name) {
			//$modal.find(".error_message").html($.t("error_contact_name_exists")).show();
			$.notify($.t("error_contact_name_exists")).show();
			console.log('Error, contact already exists with same name:'+imported_contact.name);
		    }
                    else {
			//$modal.find(".error_message").html($.t("error_contact_account_id_exists")).show();
			$.notify($.t("error_contact_account_id_exists")).show();
			console.log('Error, contact already exists with same account ID:'+imported_contact.account);
		    }
		    /*$btn.button("reset");
		      $modal.modal("unlock");*/
		}
                else {
		    BRS.database.insert("contacts", {
			name: imported_contact.name,
			email: imported_contact.email,
			account: imported_contact.account,
			accountRS: imported_contact.accountRS,
			description: imported_contact.description
		    }, function(error) {
			BRS.contacts[imported_contact.account] = {
			    name: imported_contact.name,
			    email: imported_contact.email,
			    account: imported_contact.account,
			    accountRS: imported_contact.accountRS,
			    description: imported_contact.description
			};

			setTimeout(function() {
			    /*$btn.button("reset");
			      $modal.modal("unlock");
			      $modal.modal("hide");*/
			    $.notify($.t("success_contact_add"), {
				"type": "success"
			    });

			    if (BRS.currentPage == "contacts") {
				BRS.loadPage("contacts");
			    }
                            else if (BRS.currentPage == "messages" && BRS.selectedContext) {
				var heading = BRS.selectedContext.find("h4.list-group-item-heading");
				if (heading.length) {
				    heading.html(imported_contact.name.escapeHTML());
				}
				BRS.selectedContext.data("context", "messages_sidebar_update_context");
			    }
			}, 50);
		    });
		}
	    });
	});
    }
    $("#import_contacts_button_field").css({'display':'none'});
    $("#import_contacts_button_field").on("change", function(button_event) {
	button_event.preventDefault();
	var file = $("#import_contacts_button_field")[0].files[0];
	var reader = new FileReader();
	reader.onload = function (read_event) {
	    var imported_contacts = JSON.parse(read_event.target.result);
	    BRS.importContacts(imported_contacts);
	};
	reader.readAsText(file);
	return false;
    });
    $("#import_contacts_button").on("click", function() {
	$("#import_contacts_button_field").click();
    });
    return BRS;
}(BRS || {}, jQuery));
