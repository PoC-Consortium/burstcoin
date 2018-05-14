/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.pages.peers = function() {
	BRS.sendRequest("getPeers+", {
	    "active": "true"
	}, function(response) {
	    if (response.peers && response.peers.length) {
		var peers = {};
		var nrPeers = 0;

		for (var i = 0; i < response.peers.length; i++) {
		    BRS.sendRequest("getPeer+", {
			"peer": response.peers[i]
		    }, function(peer, input) {
			if (BRS.currentPage != "peers") {
			    peers = {};
			    return;
			}

			if (!peer.errorCode) {
			    peers[input.peer] = peer;
			}

			nrPeers++;

			if (nrPeers == response.peers.length) {
			    var rows = "";
			    var uploaded = 0;
			    var downloaded = 0;
			    var connected = 0;
			    var upToDate = 0;
			    var activePeers = 0;

			    for (var i = 0; i < nrPeers; i++) {
				var peer = peers[response.peers[i]];

				if (!peer) {
				    continue;
				}

				activePeers++;
				downloaded += peer.downloadedVolume;
				uploaded += peer.uploadedVolume;
				if (peer.state == 1) {
				    connected++;
				}

				var versionToCompare = (!BRS.isTestNet ? BRS.normalVersion.versionNr : BRS.state.version);

				if (BRS.versionCompare(peer.version, versionToCompare) >= 0) {
				    upToDate++;
				}

				rows += "<tr><td>"
                                    + (peer.state == 1 ? "<i class='fas fa-check-circle' style='color:#5cb85c' title='Connected'></i>" : "<i class='fas fa-times-circle' style='color:#f0ad4e' title='Disconnected'></i>")
                                    + "&nbsp;&nbsp;"
                                    + (peer.announcedAddress ? String(peer.announcedAddress).escapeHTML() : "No name")
                                    + "</td><td"
                                    + (peer.weight > 0 ? " style='font-weight:bold'" : "")
                                    + ">" + BRS.formatWeight(peer.weight)
                                    + "</td><td>"
                                    + BRS.formatVolume(peer.downloadedVolume)
                                    + "</td><td>"
                                    + BRS.formatVolume(peer.uploadedVolume)
                                    + "</td><td><span class='label label-"
                                    + (BRS.versionCompare(peer.version, versionToCompare) >= 0 ? "success" : "danger")
                                    + "'>"
                                    + (peer.application && peer.version ? String(peer.application).escapeHTML() + " " + String(peer.version).escapeHTML() : "?")
                                    + "</label></td><td>"
                                    + (peer.platform ? String(peer.platform).escapeHTML() : "?")
                                    + "</td></tr>";
                                       
				    
			    }

			    $("#peers_uploaded_volume").html(BRS.formatVolume(uploaded)).removeClass("loading_dots");
			    $("#peers_downloaded_volume").html(BRS.formatVolume(downloaded)).removeClass("loading_dots");
			    $("#peers_connected").html(connected).removeClass("loading_dots");
			    $("#peers_up_to_date").html(upToDate + '/' + activePeers).removeClass("loading_dots");

			    BRS.dataLoaded(rows);
			}
		    });
		}
	    }
            else {
		$("#peers_uploaded_volume, #peers_downloaded_volume, #peers_connected, #peers_up_to_date").html("0").removeClass("loading_dots");
		BRS.dataLoaded();
	    }
	});
    };

    BRS.incoming.peers = function() {
	BRS.loadPage("peers");
    };

    return BRS;
}(BRS || {}, jQuery));
