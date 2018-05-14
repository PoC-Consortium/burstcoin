/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    BRS.normalVersion = {};
    BRS.betaVersion = {};
    BRS.isOutdated = false;

    BRS.checkAliasVersions = function() {
        if (BRS.downloadingBlockchain) {
            $("#brs_update_explanation span").hide();
            $("#brs_update_explanation_blockchain_sync").show();
            return;
        }
        if (BRS.isTestNet) {
            $("#brs_update_explanation span").hide();
            $("#brs_update_explanation_testnet").show();
            return;
        }

        //Get latest version nr+hash of normal version
        BRS.sendRequest("getAlias", {
            "aliasName": "nrscgversioninfo"
        }, function(response) {
            if (response.aliasURI && (response = response.aliasURI.split(" "))) {
                BRS.normalVersion.versionNr = response[0];
                BRS.normalVersion.hash = response[1];

                if (BRS.betaVersion.versionNr) {
                    BRS.checkForNewVersion();
                }
            }
        });

        //Get latest version nr+hash of beta version
        BRS.sendRequest("getAlias", {
            "aliasName": "nrscgbetaversion"
        }, function(response) {
            if (response.aliasURI && (response = response.aliasURI.split(" "))) {
                BRS.betaVersion.versionNr = response[0];
                BRS.betaVersion.hash = response[1];

                if (BRS.normalVersion.versionNr) {
                    BRS.checkForNewVersion();
                }
            }
        });

        if (BRS.inApp) {
            if (BRS.appPlatform && BRS.appVersion) {
                BRS.sendRequest("getAlias", {
                    "aliasName": "nrswallet" + BRS.appPlatform
                }, function(response) {
                    var versionInfo = $.parseJSON(response.aliasURI);

                    if (versionInfo && versionInfo.version != BRS.appVersion) {
                        var newerVersionAvailable = BRS.versionCompare(BRS.appVersion, versionInfo.version);

                        if (newerVersionAvailable == -1) {
                            parent.postMessage({
                                "type": "appUpdate",
                                "version": versionInfo.version,
                                "nrs": versionInfo.brs,
                                "hash": versionInfo.hash,
                                "url": versionInfo.url
                            }, "*");
                        }
                    }
                });
            }
            else {
                //user uses an old version which does not supply the platform / version
                var noticeDate = new Date(2016, 6, 30);

                if (new Date() > noticeDate) {

                    var downloadUrl = "https://github.com/PoC-Consortium/burstcoin/releases";

                    $("#secondary_dashboard_message").removeClass("alert-success").addClass("alert-danger").html($.t("wallet_update_available", {
                        "link": downloadUrl
                    })).show();
                }
            }
        }
    };

    BRS.checkForNewVersion = function() {
        var installVersusNormal, installVersusBeta, normalVersusBeta;

        if (BRS.normalVersion && BRS.normalVersion.versionNr) {
            installVersusNormal = BRS.versionCompare(BRS.state.version, BRS.normalVersion.versionNr);
        }
        if (BRS.betaVersion && BRS.betaVersion.versionNr) {
            installVersusBeta = BRS.versionCompare(BRS.state.version, BRS.betaVersion.versionNr);
        }

        $("#brs_update_explanation > span").hide();

        $("#brs_update_explanation_wait").attr("style", "display: none !important");

        $(".brs_new_version_nr").html(BRS.normalVersion.versionNr).show();
        $(".brs_beta_version_nr").html(BRS.betaVersion.versionNr).show();

        if (installVersusNormal == -1 && installVersusBeta == -1) {
            BRS.isOutdated = true;
            $("#brs_update").html("Outdated! (Stable & Beta)").show();
            $("#brs_update_explanation_new_choice").show();
        }
        else if (installVersusBeta == -1) {
            BRS.isOutdated = false;
            $("#brs_update").html("New Beta").show();
            $("#brs_update_explanation_new_beta").show();
        }
        else if (installVersusNormal == -1) {
            BRS.isOutdated = true;
            $("#brs_update").html("Outdated! (Stable)").show();
            $("#brs_update_explanation_new_release").show();
        }
        else {
            BRS.isOutdated = false;
            $("#brs_update_explanation_up_to_date").show();
        }
    };

    BRS.versionCompare = function(v1, v2) {
        if (v2 === undefined) {
            return -1;
        }
        else if (v1 === undefined) {
            return -1;
        }

        //https://gist.github.com/TheDistantSea/8021359 (based on)
        var v1last = v1.slice(-2);
        var v2last = v2.slice(-2);

        if (v1last == 'cg') {
            v1 = v1.substring(0, v1.length - 2);
        }
        else {
            v1last = '';
        }

        if (v2last == 'cg') {
            v2 = v2.substring(0, v2.length - 2);
        }
        else {
            v2last = '';
        }

        var v1parts = v1.split('.');
        var v2parts = v2.split('.');

        function isValidPart(x) {
            return /^\d+$/.test(x);
        }

        if (!v1parts.every(isValidPart) || !v2parts.every(isValidPart)) {
            return NaN;
        }

        v1parts = v1parts.map(Number);
        v2parts = v2parts.map(Number);

        for (var i = 0; i < v1parts.length; ++i) {
            if (v2parts.length == i) {
                return 1;
            }
            if (v1parts[i] == v2parts[i]) {
                continue;
            }
            else if (v1parts[i] > v2parts[i]) {
                return 1;
            }
            else {
                return -1;
            }
        }

        if (v1parts.length != v2parts.length) {
            return -1;
        }

        if (v1last && v2last) {
            return 0;
        }
        else if (v1last) {
            return 1;
        }
        else if (v2last) {
            return -1;
        }
        else {
            return 0;
        }
    };

    BRS.supportsUpdateVerification = function() {
        if ((typeof File !== 'undefined') && !File.prototype.slice) {
            if (File.prototype.webkitSlice) {
                File.prototype.slice = File.prototype.webkitSlice;
            }

            if (File.prototype.mozSlice) {
                File.prototype.slice = File.prototype.mozSlice;
            }
        }

        // Check for the various File API support.
        if (!window.File || !window.FileReader || !window.FileList || !window.Blob || !File.prototype.slice || !window.Worker) {
            return false;
        }

        return true;
    };

    BRS.verifyClientUpdate = function(e) {
        e.stopPropagation();
        e.preventDefault();

        var files = null;

        if (e.originalEvent.target.files && e.originalEvent.target.files.length) {
            files = e.originalEvent.target.files;
        }
        else if (e.originalEvent.dataTransfer.files && e.originalEvent.dataTransfer.files.length) {
            files = e.originalEvent.dataTransfer.files;
        }

        if (!files) {
            return;
        }

        $("#brs_update_hash_progress").css("width", "0%");
        $("#brs_update_hash_progress").show();

        var worker = new Worker("js/crypto/sha256worker.js");

        worker.onmessage = function(e) {
            if (e.data.progress) {
                $("#brs_update_hash_progress").css("width", e.data.progress + "%");
            }
            else {
                $("#brs_update_hash_progress").hide();
                $("#brs_update_drop_zone").hide();

                if (e.data.sha256 == BRS.downloadedVersion.hash) {
                    $("#brs_update_result").html($.t("success_hash_verification")).attr("class", " ");
                }
                else {
                    $("#brs_update_result").html($.t("error_hash_verification")).attr("class", "incorrect");
                }

                $("#brs_update_hash_version").html(BRS.downloadedVersion.versionNr);
                $("#brs_update_hash_download").html(e.data.sha256);
                $("#brs_update_hash_official").html(BRS.downloadedVersion.hash);
                $("#brs_update_hashes").show();
                $("#brs_update_result").show();

                BRS.downloadedVersion = {};

                $("body").off("dragover.brs, drop.brs");
            }
        };

        worker.postMessage({
            file: files[0]
        });
    };

    BRS.downloadClientUpdate = function(version) {
        if (version == "release") {
            BRS.downloadedVersion = BRS.normalVersion;
        }
        else {
            BRS.downloadedVersion = BRS.betaVersion;
        }

        if (BRS.inApp) {
            parent.postMessage({
                "type": "update",
                "update": {
                    "type": version,
                    "version": BRS.downloadedVersion.versionNr,
                    "hash": BRS.downloadedVersion.hash
                }
            }, "*");
            $("#brs_modal").modal("hide");
        }
        else {
            $("#brs_update_iframe").attr("src", "https://github.com/PoC-Consortium/burstcoin/releases/download/" + BRS.downloadedVersion.versionNr + "/burstcoin-" + BRS.downloadedVersion.versionNr + ".zip");
            $("#brs_update_explanation").hide();
            $("#brs_update_drop_zone").show();

            $("body").on("dragover.brs", function(e) {
                e.preventDefault();
                e.stopPropagation();

                if (e.originalEvent && e.originalEvent.dataTransfer) {
                    e.originalEvent.dataTransfer.dropEffect = "copy";
                }
            });

            $("body").on("drop.brs", function(e) {
                BRS.verifyClientUpdate(e);
            });

            $("#brs_update_drop_zone").on("click", function(e) {
                e.preventDefault();

                $("#brs_update_file_select").trigger("click");

            });

            $("#brs_update_file_select").on("change", function(e) {
                BRS.verifyClientUpdate(e);
            });
        }

        return false;
    };

    return BRS;
}(BRS || {}, jQuery));
