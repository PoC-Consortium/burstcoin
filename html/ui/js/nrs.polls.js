/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.pages.polls = function() {
		NRS.sendRequest("getPollIds+", function(response) {
			if (response.pollIds && response.pollIds.length) {
				var polls = {};
				var nrPolls = 0;

				for (var i = 0; i < response.pollIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": response.pollIds[i]
					}, function(poll, input) {
						if (NRS.currentPage != "polls") {
							polls = {};
							return;
						}

						if (!poll.errorCode) {
							polls[input.transaction] = poll;
						}

						nrPolls++;

						if (nrPolls == response.pollIds.length) {
							var rows = "";

							if (NRS.unconfirmedTransactions.length) {
								for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
									var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

									if (unconfirmedTransaction.type == 1 && unconfirmedTransaction.subType == 2) {
										var pollDescription = String(unconfirmedTransaction.attachment.description);

										if (pollDescription.length > 100) {
											pollDescription = pollDescription.substring(0, 100) + "...";
										}

										rows += "<tr class='tentative'><td>" + String(unconfirmedTransaction.attachment.name).escapeHTML() + "</td><td>" + pollDescription.escapeHTML() + "</td><td>" + (unconfirmedTransaction.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(unconfirmedTransaction, "sender") + "' class='user_info'>" + NRS.getAccountTitle(unconfirmedTransaction, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(unconfirmedTransaction.timestamp) + "</td><td><a href='#'>Vote (todo)</td></tr>";
									}
								}
							}

							for (var i = 0; i < nrPolls; i++) {
								var poll = polls[response.pollIds[i]];

								if (!poll) {
									continue;
								}

								var pollDescription = String(poll.attachment.description);

								if (pollDescription.length > 100) {
									pollDescription = pollDescription.substring(0, 100) + "...";
								}

								rows += "<tr><td>" + String(poll.attachment.name).escapeHTML() + "</td><td>" + pollDescription.escapeHTML() + "</td><td>" + (poll.sender != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(poll, "sender") + "' class='user_info'>" + NRS.getAccountTitle(poll, "sender") + "</a>" : "Genesis") + "</td><td>" + NRS.formatTimestamp(poll.timestamp) + "</td><td><a href='#'>Vote (todo)</td></tr>";
							}

							NRS.dataLoaded(rows);
						}
					});
				}
			} else {
				NRS.dataLoaded();
			}
		});
	}

	NRS.incoming.polls = function() {
		NRS.loadPage("polls");
	}

	$("#create_poll_answers").on("click", "button.btn.remove_answer", function(e) {
		e.preventDefault();

		if ($("#create_poll_answers > .form-group").length == 1) {
			return;
		}

		$(this).closest("div.form-group").remove();
	});

	$("#create_poll_answers_add").click(function(e) {
		var $clone = $("#create_poll_answers > .form-group").first().clone();

		$clone.find("input").val("");

		$clone.appendTo("#create_poll_answers");
	});

	NRS.forms.createPoll = function($modal) {
		var options = new Array();

		$("#create_poll_answers input.create_poll_answers").each(function() {
			var option = $.trim($(this).val());

			if (option) {
				options.push(option);
			}
		});

		if (!options.length) {
			//...
		}

		var data = {
			"name": $("#create_poll_name").val(),
			"description": $("#create_poll_description").val(),
			"optionsAreBinary": "0",
			"minNumberOfOptions": $("#create_poll_min_options").val(),
			"maxNumberOfOptions": $("#create_poll_max_options").val(),
			"feeBURST": "1",
			"deadline": "24",
			"secretPhrase": $("#create_poll_password").val()
		};

		for (var i = 0; i < options.length; i++) {
			data["option" + i] = options[i];
		}

		return {
			"requestType": "createPoll",
			"data": data
		};
	}

	NRS.forms.createPollComplete = function(response, data) {
		if (NRS.currentPage == "polls") {
			var $table = $("#polls_table tbody");

			var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0)).getTime();

			var now = parseInt(((new Date().getTime()) - date) / 1000, 10);

			var rowToAdd = "<tr class='tentative'><td>" + String(data.name).escapeHTML() + " - <strong>" + $.t("pending") + "</strong></td><td>" + String(data.description).escapeHTML() + "</td><td><a href='#' data-user='" + NRS.getAccountFormatted(NRS.accountRS) + "' class='user_info'>" + NRS.getAccountTitle(NRS.accountRS) + "</a></td><td>" + NRS.formatTimestamp(now) + "</td><td>/</td></tr>";

			$table.prepend(rowToAdd);

			if ($("#polls_table").parent().hasClass("data-empty")) {
				$("#polls_table").parent().removeClass("data-empty");
			}
		}
	}

	NRS.forms.castVote = function($modal) {

	}

	return NRS;
}(NRS || {}, jQuery));
