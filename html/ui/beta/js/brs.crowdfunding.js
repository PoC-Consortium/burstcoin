/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    var myPort = window.location.port === null || window.location.port.length === 0 ? '' : ':' + window.location.port;
    var isLoaded = false;
    BRS.pages.crowdfunding = function() {
	$("#new-project").show();
	$(".hide-all-cfs").show();	
	if (!isLoaded) {
	    isLoaded = true;
	    $.ajax({
		url: '//' + window.location.hostname.toLowerCase() + myPort + '/burst',
		type: 'POST',
		dataType: "json",
		data: "requestType=getATIds",
		success: function(data, textStatus, jqXHR) {
		    getATs(data);
		}
	    });
	}
        else {
	    BRS.pageLoaded();
	}
    }


    $("#menu-toggle").click(function(e) {
	e.preventDefault();
	$("#wrapper").toggleClass("toggled");
    });
    $('#deploy').on('show.bs.modal', function(e) {});
    $('#buy-ticket').on('click', function(e) {
	$(".alert-area").html('')
	$('.alert-area').hide();
	$('.alert-area').fadeOut('slow');
	e.preventDefault()
	e.stopPropagation()
	var totalAmount = parseInt($('#total-amount').val())
	totalAmount = totalAmount * 100000000
	var pass = $('#password').val()
	var atId = $('#at-id').val()
	var fee = 100000000
	jsonRequest = {
	    requestType: "sendMoney",
	    recipient: atId,
	    amountNQT: totalAmount,
	    feeNQT: fee,
	    secretPhrase: pass,
	    deadline: 100
	}
	$.ajax({
	    url: '//' + window.location.hostname.toLowerCase() + myPort + '/burst',
	    type: 'POST',
	    dataType: "json",
	    data: jsonRequest,
	    success: function(response, textStatus, jqXHR) {
		sendMoney(response)
	    }
	});
    });
    $('#deploy-at-btn').on('click', function(e) {
	$(".alert-area").html(' ')
	$('.alert-area').hide();
	$('.alert-area').fadeOut('slow');
	e.preventDefault()
	e.stopPropagation()
	var nameA = $('#funding-name').val()
	var descA = $('#funding-description').val()
	var weeksA = $('#sel1').val()
	var burstsA = $('#funding-amount').val()
	var burstsA = burstsA * 100000000
	var passA = $('#funding-password').val()
	var feeA = 1000000000
	var minA = 700000000
	var hexA = Number(burstsA).toString(16).toLowerCase();
	var finalA = '';
	if ((hexA.length % 2) != 0) {
	    hexA = "0" + hexA
	}
	for (var i = hexA.length - 1; i > 0; i = i - 2) {
	    finalA = finalA + hexA[i - 1] + hexA[i]
	}
	var finalLength = finalA.length
	for (var i = 0; i < 16 - finalLength; i = i + 1) {
	    finalA = finalA
	}
	var codeA = '350003000000002501000000350004020000002102000000030000004f3501030400000033040304000000' + '352501050000001b050000004a3506030600000035070304000000320a030107000000020000000000000033020406000000' + '1a2400000001070000000100000000000000320b033203043502030400000033040304000000352501050000001b05000000f235070304000000320b033203041a7c000000'
	descA = descA + 'Crowdfund'
	var dataA = '0000000000000000' + weeksA + '0000000000000000' + finalA
	$('#deploy-at-btn').attr('disabled', 'disabled')
	setTimeout(function() {
	    $('#deploy-at-btn').removeAttr('disabled')
	}, 5000);
	jsonRequest = {
	    requestType: "createATProgram",
	    name: nameA,
	    description: descA,
	    feeNQT: feeA,
	    secretPhrase: passA,
	    deadline: "1440",
	    code: codeA,
	    data: dataA,
	    dpages: "1",
	    cspages: "0",
	    uspages: "0",
	    minActivationAmountNQT: minA
	}
	$.ajax({
	    url: '//' + window.location.hostname.toLowerCase() + myPort + '/burst',
	    type: 'POST',
	    dataType: "json",
	    data: jsonRequest,
	    success: function(response, textStatus, jqXHR) {
		sendMoney(response)
	    }
	});
    });

    function filter(state) {
	$('div.col-lg-4 div.crowdbox .state').each(function() {
	    var $this = $(this);
	    if (state.indexOf("all") > -1) {
		$this.closest('div.crowdbox').parent().fadeIn();
		$this.closest('div.crowdbox').parent().css('display', 'visible')
	    }
            else if ($this.text().toLowerCase().indexOf(state) === -1) {
		$this.closest('div.crowdbox').parent().fadeOut();
		$this.closest('div.crowdbox').parent().css('display', 'none')
	    }
            else {
		$this.closest('div.crowdbox').parent().fadeIn();
		$this.closest('div.crowdbox').parent().css('display', 'visible')
	    }
	});
    }

    function sendMoney(response) {
	if (("errorCode" in response)) {
	    $('.alert-area').show();
	    $('.alert-area').fadeIn('slow');
	    $(".alert-area").html('<div class="alert alert-danger" role="alert"> Error sending transaction. </div>')
	    $('.alert-area').hide();
	    $('.alert-area').fadeIn('slow');
	}
        else {
	    $('.alert-area').show();
	    $('.alert-area').fadeIn('slow');
	    $(".alert-area").html('<div class="alert alert-success" role="alert"> Transaction has been processed successfully. </div>')
	    $('.alert-area').hide();
	    $('.alert-area').fadeIn('slow');
	}
    }
    $('#buyTicket').on('show.bs.modal', function(e) {
	var totalAmount = $(e.relatedTarget).data('total-amount');
	var atId = $(e.relatedTarget).data('at-id');
	$(e.currentTarget).find('input[name="total-amount"]').val(totalAmount);
	$(e.currentTarget).find('input[name="at-id"]').val(atId);
    });

    function getATs(data) {
	getBlockHeight(data.atIds);

        /**
         * Hidden CFs Management while loading the page.
         */
        
        var cfs = {"crowdfund" : []};
        setTimeout(function () { 
            if (localStorage.getItem("cfs") === null) {
                // check if data is set in the local storage or not.
                cfs = {"crowdfund" : []};
                localStorage.setItem('cfs', JSON.stringify(cfs));
            }else{
                cfs = JSON.parse(localStorage.getItem("cfs"));
                // iterate through each of the cf to hide it.
                jQuery.each(cfs.crowdfund, function() {
                    creator = $(this)[0].creator;
                    name = $(this)[0].name;

                    $("." + creator +"[name='"+ name +"']").addClass('temp-class');
                    $("." + creator +"[name='"+ name +"']").attr('hidden-cf', 'yes');
                    $("." + creator +"[name='"+ name +"']").hide();
                    $("." + creator +"[name='"+ name +"'] .close").html('<i style="color: green;" class="fa fa-plus-circle" aria-hidden="true"></i>');
                    $("." + creator +"[name='"+ name +"'] .close").removeClass('hide-cf');
                    $("." + creator +"[name='"+ name +"'] .close").addClass('show-cf');
                    
                    $("." + creator +"[name='"+ name +"'] .hide-all-cfs").html("Show All CFs from this creator");
                    $("." + creator +"[name='"+ name +"'] .hide-all-cfs").removeClass("btn-primary");
                    $("." + creator +"[name='"+ name +"'] .hide-all-cfs").addClass("btn-success");
                    $("." + creator +"[name='"+ name +"'] .hide-all-cfs").addClass("show-all-cfs");
                    $("." + creator +"[name='"+ name +"'] .hide-all-cfs").removeClass("hide-all-cfs");
                });
            }
        }, 2000);
    }

    function getAT(blockHeight, atId) {
	var jsonRequest = {
	    requestType: "getAT",
	    at: atId
	}
	$.ajax({
	    url: '//' + window.location.hostname.toLowerCase() + myPort + '/burst',
	    type: 'POST',
	    dataType: "json",
	    data: jsonRequest,
	    success: function(data, textStatus, jqXHR) {
		getTransaction(blockHeight, data);
	    }
	});
    }

    function getBlockHeight(atIds) {
	var jsonRequest = {
	    requestType: "getBlockchainStatus"
	}
	$.ajax({
	    url: '//' + window.location.hostname.toLowerCase() + myPort + '/burst',
	    type: 'POST',
	    dataType: "json",
	    data: jsonRequest,
	    success: function(data, textStatus, jqXHR) {
		for (var i = 0; i < atIds.length; i++) {
		    getAT(data.numberOfBlocks, atIds[i]);
		}
	    }
	});
    }

    function getTransaction(blockHeight, atData) {
	var hexString = atData.machineData.substring(1 * 8, 1 * 8 + 8);
	var decSting = converters.hexStringToDecString(hexString);
	getDecision(decSting, blockHeight, atData);
    }

    function getDecision(transaction, blockHeight, atData) {
	var hexString = atData.machineData.substring(1 * 16, 1 * 16 + 16);
	var decSting = converters.hexStringToDecString(hexString);
	getTargetAmount(decSting, transaction, blockHeight, atData);
    }

    function getTargetAmount(decision, transaction, blockHeight, atData) {
	var hexString = atData.machineData.substring(3 * 16, 3 * 16 + 16);
	var decSting = converters.hexStringToDecString(hexString);
	getGatheredAmount(decSting, decision, transaction, blockHeight, atData);
    }

    function getGatheredAmount(targetAmount, decision, transaction, blockHeight, atData) {
	var hexString = atData.machineData.substring(2 * 16, 2 * 16 + 16);
	var decSting = converters.hexStringToDecString(hexString);
	getFunded(decSting, targetAmount, decision, transaction, blockHeight, atData);
    }

    function getFunded(gatheredAmount, targetAmount, decision, transaction, blockHeight, atData) {
	var hexString = atData.machineData.substring(7 * 16, 7 * 16 + 16);
	var decSting = converters.hexStringToDecString(hexString);
	drawAT(decSting, gatheredAmount, targetAmount, decision, transaction, blockHeight, atData);
    }

    function drawAT(funded, gatheredAmount, targetAmount, decision, transaction, blockHeight, atData) {
	var tarAmount = 0
	var atBalance = 0
	if (targetAmount.length > 8) {
	    tarAmount = parseInt(targetAmount.substring(0, targetAmount.length - 8))
	}
	if (atData.balanceNQT.length > 8) {
	    atBalance = parseInt(atData.balanceNQT.substring(0, atData.balanceNQT.length - 8))
	}
	if (funded == 1 || funded == 2) {
	    if (gatheredAmount.length > 8) {
		atBalance = parseInt(gatheredAmount.substring(0, gatheredAmount.length - 8))
	    }
	}
	var ratio = atBalance / tarAmount * 100
	var ratioText = ratio - 15
	var ratioDesc = ratio.toFixed(1) + '%'
	if (ratio > 100) {
	    ratioText = 80
	    ratioDesc = 'Funded!'
	}
	var blocks = 'blocks to go'
	var descr = atData.description.substr(0, atData.description.length - 9);
	var ends = parseInt(transaction) + parseInt(decision) - parseInt(blockHeight)
	var diff = parseInt(blockHeight) - parseInt(transaction)
	var fundedStr = 'ongoing'
	var color = 'white'
	var icon = 'glyphicon glyphicon-signal'
	var finished = ''
	var buttonStr = 'Pledge'
	var buttonState = ''
	if (funded == 2) {
	    fundedStr = 'false'
	    ratioDesc = 'Not funded!'
	    icon = 'glyphicon glyphicon-remove-sign'
	    ratioText = 40
	    color = 'black'
	    finished = 'finished'
	    blocks = 'blocks ago'
	    buttonState = 'disabled'
	}
        else if (funded == 1) {
	    fundedStr = 'true'
	    ratioDesc = 'Successfully Funded!'
	    icon = 'glyphicon glyphicon-ok-sign'
	    ratioText = 40
	    blocks = 'blocks ago'
	    finished = 'finished'
	    buttonStr = 'Donate'
	}
	if (funded == 0 && ends < 0) {
	    ends = 'NaN'
	    tarAmount = 'NaN'
	}
        else {
	    ends = Math.abs(ends)
	}
	var html = '<div class="col-lg-4 crowd '+ atData.creatorRS +'" name="'+ atData.name +'" creator="'+ atData.creatorRS +'">' + '<div class="crowdbox"> ' + '<a href="#" class="close hide-cf" data-dismiss="alert" aria-label="close"><i style="color: red;" class="fa fa-minus-circle" aria-hidden="true"></i></a><h2 class="head">' + atData.name + '</h2>' + '<hr>' + '<div class="crowdtext">' + descr + '</div>' + '<div class="state" style="display:none">' + fundedStr + '</div>' + '<hr>' + '<div class="progress">' + '<span class="progress-value" style="color:' + color + ';left:' + ratioText.toFixed(2) + '% "> <span class="' + icon + '"></span><span>  ' + ratioDesc + '</span></span>' + '<div class="progress-bar" style="width:' + ratio + '%"></div>' + '</div>' + '<div class="text-amount">' + '<div class="row">' + '<div class="col-lg-5">' + '   <span style=' + '   "font-size:1.2em;color:black">' + atBalance + '</span>' + '   <span style="font-size:0.9em;color:gray">pledged out of ' + tarAmount + '</span>' + '</div>' + '<div class="col-lg-4">' + '   <span><span style=' + '   "font-size:0.9em;color:gray">' + finished + ' </span><span style=' + '   "font-size:1.2em;color:black">' + ends + '</span> <span style=' + '   "font-size:0.9em;color:gray">' + blocks + '</span> </span>' + '</div>' + '<div class="col-lg-3">' + '   <span><span style=' + '   "font-size:1.2em;color:black">' + ratio.toFixed(2) + '%</span> <span style=' + '   "font-size:0.9em;color:gray">funded</span></span>' + '</div>' + '</div>' + '</div>' + '<hr>' + '<div class="row">' + '<div class="col-lg-12"><strong>Creator: </strong> <span style=' + '"font-size:1.0 em;color:gray">' + atData.creatorRS + '</span></div>' + '<div class="row" style="margin-top: 32px;"><div class="" style="margin: 0 30px;"><button type="button" class="btn btn-primary btn-block hide-all-cfs btn-sm test" style="font-size: 13px; margin-right: 10px; width: auto;" creator-val="'+ atData.creatorRS +'">Hide All CFs from this creator</button>'
	
        $(document).ready(function(e) {
            $(".crowdfunding-link-notfunded").click(function(e) {		
		$(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").html("Hide All CFs from this creator");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").removeClass("btn-success");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("btn-primary");
            });
            $(".crowdfunding-link").click(function(e) {
		$(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").html("Hide All CFs from this creator");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").removeClass("btn-success");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("btn-primary");
            });
            $(".crowdfunding-link-active").click(function(e) {
		$(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").html("Hide All CFs from this creator");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").removeClass("btn-success");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("btn-primary");
            });
            $(".crowdfunding-link-funded").click(function(e) {
		$(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").html("Hide All CFs from this creator");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").removeClass("btn-success");
		$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("btn-primary");
            });
        });


	if (buttonState != 'disabled') {
	    html = html + '<a data-toggle="modal" data-at-id="' + atData.at + '" data-total-amount="1000" title="Add this item" class="open-buyTicket btn btn-primary btn-sm" font-size: 13px; href="#buyTicket" style="position: relative;right: -205px;top: -35px;"><i class="glyphicon glyphicon-send"></i>&nbsp;' + buttonStr + ' </a></div>'
	}
	html = html + '</div>' + '</div>' + '</div>' + '</div>'
	$('.at-block').append(html);
    }
    $(document).ready(function(e) {
	$('.form-search').on('submit', function() {
	    return false;
	});
	$('#search-btn').on('click', function(e) {
	    var query = $.trim($(this).parent().prevAll('.search-query').val()).toLowerCase();
	    $('div.col-lg-4 div.crowdbox .crowdtext').each(function() {
		var $this = $(this);
		var h2Text = $this.closest('div.crowdbox').parent().find('h2')
		if ($this.text().toLowerCase().indexOf(query) === -1 && h2Text.text().toLowerCase().indexOf(query) === -1) {
		    $this.closest('div.crowdbox').parent().fadeOut();
		    $this.closest('div.crowdbox').parent().css('display', 'none')
		}
                else {
		    $this.closest('div.crowdbox').parent().fadeIn();
		    $this.closest('div.crowdbox').parent().css('display', 'visible')
		}
	    });
	});
	
        /**
         * Ceowd funding tab click event
         */
	$(".crowdfunding-link").click(function(e) {
	    $("#crowdfunding_page").show();
	    filter("all");
            // show hidden cfs and hide show cfs
	    $(".crowd").each(function(){
		var hidden_status = $(this).attr('hidden-cf');
                if(hidden_status == null){
                    $(this).show();
                }
                if(hidden_status == 'yes'){
                    $(this).hide();
                }
	    });
	});
        // filter active
	$(".crowdfunding-link-active").click(function(e) {
	    $("#crowdfunding_page").show();
            $(".crowd").show();
            $(".hide-all-cfs").show();
	    filter("ongoing");
            // hide already hidden elements.
            $(".crowd:visible").each(function(e){
                var hidden_status = $(this).attr('hidden-cf');
                if(hidden_status == 'yes'){
                    $(this).hide();
                }
            });
	});
        // filter funded
	$(".crowdfunding-link-funded").click(function(e) {
	    
	    $("#crowdfunding_page").show();
            $(".crowd").show();
            $(".hide-all-cfs").show();
	    filter("true");
            // hide already hidden elements.
            $(".crowd:visible").each(function(e){
                var hidden_status = $(this).attr('hidden-cf');
                if(hidden_status == 'yes'){
                    $(this).hide();
                }
            });
	});
        // filter not funded
	$(".crowdfunding-link-notfunded").click(function(e) {
	    $("#crowdfunding_page").show();
            $(".crowd").show();
            $(".hide-all-cfs").show();
	    filter("false");
            // hide already hidden elements.
            $(".crowd:visible").each(function(e){
                var hidden_status = $(this).attr('hidden-cf');
                if(hidden_status == 'yes'){
                    $(this).hide();
                    //$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("show-all-cfs");
                    //$(".btn.btn-block.hide-all-cfs.btn-sm.test").removeClass("hide-all-cfs");
                }
            });
	});

        $(".sidebar-menu").on("click", function(e){
	    if (!$("li#sidebar_crowdfunding").hasClass('active')){
                $("#new-project").hide();
            }
        });
        
        /**
         * Hide CFs
         */
        $("body").on("click", ".crowd .hide-cf", function(e){
            e.preventDefault();
            $(this).closest(".crowd").attr('hidden-cf', 'yes');
            $(this).closest(".crowd").hide();
            $(this).html('<i style="color: green;" class="fa fa-plus-circle" aria-hidden="true"></i>');
            $(this).removeClass('hide-cf');
            $(this).addClass('show-cf');
            // set the value into the local storage.
            name = $(this).closest(".crowd").attr('name');
            creator = $(this).closest(".crowd").attr('creator');
            set_localstorage_val(creator, name);
            return false;
        });
        
        $("body").on("click", ".crowd .show-cf", function(e){
            e.preventDefault();
            $(this).closest(".crowd").removeAttr('hidden-cf');
            $(this).closest(".crowd").hide();
            $(this).html('<i style="color: red;" class="fa fa-minus-circle" aria-hidden="true"></i>');
            $(this).removeClass('show-cf');
            $(this).addClass('hide-cf');
            
            name = $(this).closest(".crowd").attr('name');
            creator = $(this).closest(".crowd").attr('creator');
            
            // Remove cfs from the local storage.
            var cfs = JSON.parse(localStorage.getItem("cfs"));
            $.each(cfs.crowdfund, function(i, el){
                if (this.name == name && this.creator == creator){
                    cfs.crowdfund.splice(i, 1);
                    localStorage.setItem('cfs', JSON.stringify(cfs));
                    console.log(cfs);
                }
            });
            return false;
        });
        /**
         * Hide all cfs
         */
        $("body").on("click", ".hide-all-cfs", function(e){
            var creator = $(this).attr('creator-val');
            $("." + creator).attr('hidden-cf', 'yes');
            $("." + creator).hide();
            $("." + creator + " .close").addClass('show-cf');
            $("." + creator + " .close").removeClass('hide-cf');
            $("." + creator + " .close").html('<i style="color: green;" class="fa fa-plus-circle" aria-hidden="true"></i>');
            $("." + creator + " .close").removeClass('hide-cf');
            $("." + creator + " .close").addClass('show-cf');
            $("." + creator + " .hide-all-cfs").html("Show All CFs from this creator");
	    $("." + creator + " .hide-all-cfs").removeClass("btn-primary");
	    $("." + creator + " .hide-all-cfs").addClass("btn-success");
            $("." + creator + " .hide-all-cfs").addClass('show-all-cfs');
            $("." + creator + " .hide-all-cfs").removeClass('hide-all-cfs');
            // set the value into the local storage.
            $("." + creator).each(function (index, value) {
                name = $(this).attr('name');
                set_localstorage_val(creator, name);
            });
            console.log(JSON.parse(localStorage.getItem("cfs")));
            return false;
        });
        
        /**
         * Show all cfs
         */
        $("body").on("click", ".show-all-cfs", function(e){
            var creator = $(this).attr('creator-val');
            $("." + creator).removeAttr('hidden-cf');
            $("." + creator).hide();
            $("." + creator + " .close").removeClass('show-cf');
            $("." + creator + " .close").addClass('hide-cf');
            $("." + creator + " .close").html('<i style="color: red;" class="fa fa-minus-circle" aria-hidden="true"></i>');
            $("." + creator + " .close").removeClass('hide-cf');
            $("." + creator + " .close").addClass('show-cf');
            $("." + creator + " .show-all-cfs").html("Hide All CFs from this creator");
	    $("." + creator + " .show-all-cfs").removeClass("btn-success");
	    $("." + creator + " .show-all-cfs").addClass("btn-primary");
            $("." + creator + " .show-all-cfs").addClass('hide-all-cfs');
            $("." + creator + " .show-all-cfs").removeClass('show-all-cfs');
            // remove the value from the local storage.
            $("." + creator).each(function (index, value) {
                name = $(this).attr('name');
                console.log(creator + " : " + name);
                var cfs = JSON.parse(localStorage.getItem("cfs"));
                $.each(cfs.crowdfund, function(i, el){
                    if (this.name == name && this.creator == creator){
                        cfs.crowdfund.splice(i, 1);
                        //console.log(cfs);
                        localStorage.setItem('cfs', JSON.stringify(cfs));
                    }
                });
            });
            console.log(JSON.parse(localStorage.getItem("cfs")));
            return false;
        });
        
        /**
         * Show hidden CFs  btn-success
         */
        $(".crowdfunding-link-hidden").click(function(e) {
	    $(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").html("Show All CFs from this creator");
	    $(".btn.btn-primary.btn-block.hide-all-cfs.btn-sm.test").removeClass("btn-primary");
	    $(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("btn-success");
            //$(".btn.btn-block.hide-all-cfs.btn-sm.test").addClass("show-all-cfs");
            //$(".btn.btn-block.show-all-cfs.btn-sm.test").removeClass("hide-all-cfs");
	    
	    $("#crowdfunding_page").show();
            $(".hide-all-cfs").css('display', 'none !important');
            // show hidden cfs and hide shown cfs
	    $(".crowd").each(function(){
		var hidden_status = $(this).attr('hidden-cf');
                if(hidden_status == null){
                    $(this).hide();
                }
                if(hidden_status == 'yes'){
                    $(this).show();
                }
	    });
	});
    });

    /**
     * This method will be used to set the value in local storage object.
     */
    function set_localstorage_val(creator, name){
        // get the existing cf values from local storage
        var cf = JSON.parse(localStorage.getItem("cfs"));
        // put the values in the json object
        cf.crowdfund.push({"creator": creator, "name": name});
        // put the values in the local storage
        localStorage.setItem('cfs', JSON.stringify(cf));
        //console.log(localStorage.getItem('cfs'));
    }

    
    return BRS;
}(BRS || {}, jQuery));
