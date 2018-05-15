/**
 * @depends {brs.js}
 */
var BRS = (function(BRS, $, undefined) {
    var LOCALE_DATE_FORMATS = {
        "ar-SA": "dd/MM/yy",
        "bg-BG": "dd.M.yyyy",
        "ca-ES": "dd/MM/yyyy",
        "zh-TW": "yyyy/M/d",
        "cs-CZ": "d.M.yyyy",
        "da-DK": "dd-MM-yyyy",
        "de-DE": "dd.MM.yyyy",
        "el-GR": "d/M/yyyy",
        "en-US": "M/d/yyyy",
        "fi-FI": "d.M.yyyy",
        "fr-FR": "dd/MM/yyyy",
        "he-IL": "dd/MM/yyyy",
        "hu-HU": "yyyy. MM. dd.",
        "is-IS": "d.M.yyyy",
        "it-IT": "dd/MM/yyyy",
        "ja-JP": "yyyy/MM/dd",
        "ko-KR": "yyyy-MM-dd",
        "nl-NL": "d-M-yyyy",
        "nb-NO": "dd.MM.yyyy",
        "pl-PL": "yyyy-MM-dd",
        "pt-BR": "d/M/yyyy",
        "ro-RO": "dd.MM.yyyy",
        "ru-RU": "dd.MM.yyyy",
        "hr-HR": "d.M.yyyy",
        "sk-SK": "d. M. yyyy",
        "sq-AL": "yyyy-MM-dd",
        "sv-SE": "yyyy-MM-dd",
        "th-TH": "d/M/yyyy",
        "tr-TR": "dd.MM.yyyy",
        "ur-PK": "dd/MM/yyyy",
        "id-ID": "dd/MM/yyyy",
        "uk-UA": "dd.MM.yyyy",
        "be-BY": "dd.MM.yyyy",
        "sl-SI": "d.M.yyyy",
        "et-EE": "d.MM.yyyy",
        "lv-LV": "yyyy.MM.dd.",
        "lt-LT": "yyyy.MM.dd",
        "fa-IR": "MM/dd/yyyy",
        "vi-VN": "dd/MM/yyyy",
        "hy-AM": "dd.MM.yyyy",
        "az-Latn-AZ": "dd.MM.yyyy",
        "eu-ES": "yyyy/MM/dd",
        "mk-MK": "dd.MM.yyyy",
        "af-ZA": "yyyy/MM/dd",
        "ka-GE": "dd.MM.yyyy",
        "fo-FO": "dd-MM-yyyy",
        "hi-IN": "dd-MM-yyyy",
        "ms-MY": "dd/MM/yyyy",
        "kk-KZ": "dd.MM.yyyy",
        "ky-KG": "dd.MM.yy",
        "sw-KE": "M/d/yyyy",
        "uz-Latn-UZ": "dd/MM yyyy",
        "tt-RU": "dd.MM.yyyy",
        "pa-IN": "dd-MM-yy",
        "gu-IN": "dd-MM-yy",
        "ta-IN": "dd-MM-yyyy",
        "te-IN": "dd-MM-yy",
        "kn-IN": "dd-MM-yy",
        "mr-IN": "dd-MM-yyyy",
        "sa-IN": "dd-MM-yyyy",
        "mn-MN": "yy.MM.dd",
        "gl-ES": "dd/MM/yy",
        "kok-IN": "dd-MM-yyyy",
        "syr-SY": "dd/MM/yyyy",
        "dv-MV": "dd/MM/yy",
        "ar-IQ": "dd/MM/yyyy",
        "zh-CN": "yyyy/M/d",
        "de-CH": "dd.MM.yyyy",
        "en-GB": "dd/MM/yyyy",
        "es-MX": "dd/MM/yyyy",
        "fr-BE": "d/MM/yyyy",
        "it-CH": "dd.MM.yyyy",
        "nl-BE": "d/MM/yyyy",
        "nn-NO": "dd.MM.yyyy",
        "pt-PT": "dd-MM-yyyy",
        "sr-Latn-CS": "d.M.yyyy",
        "sv-FI": "d.M.yyyy",
        "az-Cyrl-AZ": "dd.MM.yyyy",
        "ms-BN": "dd/MM/yyyy",
        "uz-Cyrl-UZ": "dd.MM.yyyy",
        "ar-EG": "dd/MM/yyyy",
        "zh-HK": "d/M/yyyy",
        "de-AT": "dd.MM.yyyy",
        "en-AU": "d/MM/yyyy",
        "es-ES": "dd/MM/yyyy",
        "fr-CA": "yyyy-MM-dd",
        "sr-Cyrl-CS": "d.M.yyyy",
        "ar-LY": "dd/MM/yyyy",
        "zh-SG": "d/M/yyyy",
        "de-LU": "dd.MM.yyyy",
        "en-CA": "dd/MM/yyyy",
        "es-GT": "dd/MM/yyyy",
        "fr-CH": "dd.MM.yyyy",
        "ar-DZ": "dd-MM-yyyy",
        "zh-MO": "d/M/yyyy",
        "de-LI": "dd.MM.yyyy",
        "en-NZ": "d/MM/yyyy",
        "es-CR": "dd/MM/yyyy",
        "fr-LU": "dd/MM/yyyy",
        "ar-MA": "dd-MM-yyyy",
        "en-IE": "dd/MM/yyyy",
        "es-PA": "MM/dd/yyyy",
        "fr-MC": "dd/MM/yyyy",
        "ar-TN": "dd-MM-yyyy",
        "en-ZA": "yyyy/MM/dd",
        "es-DO": "dd/MM/yyyy",
        "ar-OM": "dd/MM/yyyy",
        "en-JM": "dd/MM/yyyy",
        "es-VE": "dd/MM/yyyy",
        "ar-YE": "dd/MM/yyyy",
        "en-029": "MM/dd/yyyy",
        "es-CO": "dd/MM/yyyy",
        "ar-SY": "dd/MM/yyyy",
        "en-BZ": "dd/MM/yyyy",
        "es-PE": "dd/MM/yyyy",
        "ar-JO": "dd/MM/yyyy",
        "en-TT": "dd/MM/yyyy",
        "es-AR": "dd/MM/yyyy",
        "ar-LB": "dd/MM/yyyy",
        "en-ZW": "M/d/yyyy",
        "es-EC": "dd/MM/yyyy",
        "ar-KW": "dd/MM/yyyy",
        "en-PH": "M/d/yyyy",
        "es-CL": "dd-MM-yyyy",
        "ar-AE": "dd/MM/yyyy",
        "es-UY": "dd/MM/yyyy",
        "ar-BH": "dd/MM/yyyy",
        "es-PY": "dd/MM/yyyy",
        "ar-QA": "dd/MM/yyyy",
        "es-BO": "dd/MM/yyyy",
        "es-SV": "dd/MM/yyyy",
        "es-HN": "dd/MM/yyyy",
        "es-NI": "dd/MM/yyyy",
        "es-PR": "dd/MM/yyyy",
        "am-ET": "d/M/yyyy",
        "tzm-Latn-DZ": "dd-MM-yyyy",
        "iu-Latn-CA": "d/MM/yyyy",
        "sma-NO": "dd.MM.yyyy",
        "mn-Mong-CN": "yyyy/M/d",
        "gd-GB": "dd/MM/yyyy",
        "en-MY": "d/M/yyyy",
        "prs-AF": "dd/MM/yy",
        "bn-BD": "dd-MM-yy",
        "wo-SN": "dd/MM/yyyy",
        "rw-RW": "M/d/yyyy",
        "qut-GT": "dd/MM/yyyy",
        "sah-RU": "MM.dd.yyyy",
        "gsw-FR": "dd/MM/yyyy",
        "co-FR": "dd/MM/yyyy",
        "oc-FR": "dd/MM/yyyy",
        "mi-NZ": "dd/MM/yyyy",
        "ga-IE": "dd/MM/yyyy",
        "se-SE": "yyyy-MM-dd",
        "br-FR": "dd/MM/yyyy",
        "smn-FI": "d.M.yyyy",
        "moh-CA": "M/d/yyyy",
        "arn-CL": "dd-MM-yyyy",
        "ii-CN": "yyyy/M/d",
        "dsb-DE": "d. M. yyyy",
        "ig-NG": "d/M/yyyy",
        "kl-GL": "dd-MM-yyyy",
        "lb-LU": "dd/MM/yyyy",
        "ba-RU": "dd.MM.yy",
        "nso-ZA": "yyyy/MM/dd",
        "quz-BO": "dd/MM/yyyy",
        "yo-NG": "d/M/yyyy",
        "ha-Latn-NG": "d/M/yyyy",
        "fil-PH": "M/d/yyyy",
        "ps-AF": "dd/MM/yy",
        "fy-NL": "d-M-yyyy",
        "ne-NP": "M/d/yyyy",
        "se-NO": "dd.MM.yyyy",
        "iu-Cans-CA": "d/M/yyyy",
        "sr-Latn-RS": "d.M.yyyy",
        "si-LK": "yyyy-MM-dd",
        "sr-Cyrl-RS": "d.M.yyyy",
        "lo-LA": "dd/MM/yyyy",
        "km-KH": "yyyy-MM-dd",
        "cy-GB": "dd/MM/yyyy",
        "bo-CN": "yyyy/M/d",
        "sms-FI": "d.M.yyyy",
        "as-IN": "dd-MM-yyyy",
        "ml-IN": "dd-MM-yy",
        "en-IN": "dd-MM-yyyy",
        "or-IN": "dd-MM-yy",
        "bn-IN": "dd-MM-yy",
        "tk-TM": "dd.MM.yy",
        "bs-Latn-BA": "d.M.yyyy",
        "mt-MT": "dd/MM/yyyy",
        "sr-Cyrl-ME": "d.M.yyyy",
        "se-FI": "d.M.yyyy",
        "zu-ZA": "yyyy/MM/dd",
        "xh-ZA": "yyyy/MM/dd",
        "tn-ZA": "yyyy/MM/dd",
        "hsb-DE": "d. M. yyyy",
        "bs-Cyrl-BA": "d.M.yyyy",
        "tg-Cyrl-TJ": "dd.MM.yy",
        "sr-Latn-BA": "d.M.yyyy",
        "smj-NO": "dd.MM.yyyy",
        "rm-CH": "dd/MM/yyyy",
        "smj-SE": "yyyy-MM-dd",
        "quz-EC": "dd/MM/yyyy",
        "quz-PE": "dd/MM/yyyy",
        "hr-BA": "d.M.yyyy.",
        "sr-Latn-ME": "d.M.yyyy",
        "sma-SE": "yyyy-MM-dd",
        "en-SG": "d/M/yyyy",
        "ug-CN": "yyyy-M-d",
        "sr-Cyrl-BA": "d.M.yyyy",
        "es-US": "M/d/yyyy"
    };

    var LANG = window.navigator.userLanguage || window.navigator.language;

    var LOCALE_DATE_FORMAT = LOCALE_DATE_FORMATS[LANG] || 'dd/MM/yyyy';

    BRS.formatVolume = function(volume) {
        var sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        if (volume === 0) return '0 B';
        var i = parseInt(Math.floor(Math.log(volume) / Math.log(1024)));

        volume = Math.round(volume / Math.pow(1024, i), 2);
        var size = sizes[i];

        var digits = [],
            formattedVolume = "",
            i;
        do {
            digits[digits.length] = volume % 10;
            volume = Math.floor(volume / 10);
        } while (volume > 0);
        for (i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 === 0) {
                formattedVolume = "'" + formattedVolume;
            }
            formattedVolume = digits[i] + formattedVolume;
        }
        return formattedVolume + " " + size;
    };

    BRS.formatWeight = function(weight) {
        var digits = [],
            formattedWeight = "",
            i;
        do {
            digits[digits.length] = weight % 10;
            weight = Math.floor(weight / 10);
        } while (weight > 0);
        for (i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 === 0) {
                formattedWeight = "'" + formattedWeight;
            }
            formattedWeight = digits[i] + formattedWeight;
        }
        return formattedWeight.escapeHTML();
    };

    BRS.formatOrderPricePerWholeQNT = function(price, decimals) {
        price = BRS.calculateOrderPricePerWholeQNT(price, decimals, true);

        return BRS.format(price);
    };

    BRS.calculateOrderPricePerWholeQNT = function(price, decimals, returnAsObject) {
        if (typeof price !== "object") {
            price = new BigInteger(String(price));
        }

        return BRS.convertToNXT(price.multiply(new BigInteger("" + Math.pow(10, decimals))), returnAsObject);
    };

    BRS.calculatePricePerWholeQNT = function(price, decimals) {
        price = String(price);

        if (decimals) {
            var toRemove = price.slice(-decimals);

            if (!/^[0]+$/.test(toRemove)) {
                //return new Big(price).div(new Big(Math.pow(10, decimals))).round(8, 0);
                throw "Invalid input.";
            } else {
                return price.slice(0, -decimals);
            }
        } else {
            return price;
        }
    };

    BRS.calculateOrderTotalNQT = function(quantityQNT, priceNQT) {
        if (typeof quantityQNT !== "object") {
            quantityQNT = new BigInteger(String(quantityQNT));
        }

        if (typeof priceNQT !== "object") {
            priceNQT = new BigInteger(String(priceNQT));
        }

        var orderTotal = quantityQNT.multiply(priceNQT);

        return orderTotal.toString();
    };

    BRS.calculateOrderTotal = function(quantityQNT, priceNQT) {
        if (typeof quantityQNT !== "object") {
            quantityQNT = new BigInteger(String(quantityQNT));
        }

        if (typeof priceNQT !== "object") {
            priceNQT = new BigInteger(String(priceNQT));
        }

        return BRS.convertToNXT(quantityQNT.multiply(priceNQT));
    };

    BRS.calculatePercentage = function(a, b) {
        a = new Big(String(a));
        b = new Big(String(b));

        var result = a.div(b).times(new Big("100")).toFixed(2);

        return result.toString();
    };

    BRS.convertToNXT = function(amount, returnAsObject) {
        var negative = "";
        var afterComma = "";

        if (typeof amount !== "object") {
            amount = new BigInteger(String(amount));
        }

        var fractionalPart = amount.mod(new BigInteger("100000000")).toString(); //.replace(/0+$/, ""); //todo: check if equal to zero first

        amount = amount.divide(new BigInteger("100000000"));

        if (amount.compareTo(BigInteger.ZERO) < 0) {
            amount = amount.abs();
            negative = "-";
        }

        if (fractionalPart && fractionalPart !== "0") {
            afterComma = ".";

            for (var i = fractionalPart.length; i < 8; i++) {
                afterComma += "0";
            }

            afterComma += fractionalPart.replace(/0+$/, "");
        }

        amount = amount.toString();

        if (returnAsObject) {
            return {
                "negative": negative,
                "amount": amount,
                "afterComma": afterComma
            };
        } else {
            return negative + amount + afterComma;
        }
    };

    BRS.amountToPrecision = function(amount, decimals) {
        amount = String(amount);

        var parts = amount.split(".");

        //no fractional part
        if (parts.length === 1) {
            return parts[0];
        } else if (parts.length === 2) {
            var fraction = parts[1];
            fraction = fraction.replace(/0+$/, "");

            if (fraction.length > decimals) {
                fraction = fraction.substring(0, decimals);
            }

            return parts[0] + "." + fraction;
        } else {
            throw "Incorrect input";
        }
    };
    BRS.convertToNQT = function(currency) {
        currency = String(currency);

        var parts = currency.split(".");

        var amount = parts[0];

        //no fractional part
        var fraction;
        if (parts.length === 1) {
            fraction = "00000000";
        } else if (parts.length === 2) {
            if (parts[1].length <= 8) {
                fraction = parts[1];
            } else {
                fraction = parts[1].substring(0, 8);
            }
        } else {
            throw "Invalid input";
        }
        for (var i = fraction.length; i < 8; i++) {
            fraction += "0";
        }

        var result = amount + "" + fraction;

        //in case there's a comma or something else in there.. at this point there should only be numbers
        if (!/^\d+$/.test(result)) {
            throw "Invalid input.";
        }

        //remove leading zeroes
        result = result.replace(/^0+/, "");

        if (result === "") {
            result = "0";
        }

        return result;
    };

    BRS.convertToQNTf = function(quantity, decimals, returnAsObject) {
        quantity = String(quantity);

        if (quantity.length < decimals) {
            for (var i = quantity.length; i < decimals; i++) {
                quantity = "0" + quantity;
            }
        }

        var afterComma = "";

        if (decimals) {
            afterComma = "." + quantity.substring(quantity.length - decimals);
            quantity = quantity.substring(0, quantity.length - decimals);

            if (!quantity) {
                quantity = "0";
            }

            afterComma = afterComma.replace(/0+$/, "");

            if (afterComma === ".") {
                afterComma = "";
            }
        }

        if (returnAsObject) {
            return {
                "amount": quantity,
                "afterComma": afterComma
            };
        } else {
            return quantity + afterComma;
        }
    };

    BRS.convertToQNT = function(quantity, decimals) {
        quantity = String(quantity);

        var parts = quantity.split(".");

        var qnt = parts[0];

        //no fractional part
        if (parts.length === 1) {
            if (decimals) {
                for (var i = 0; i < decimals; i++) {
                    qnt += "0";
                }
            }
        } else if (parts.length === 2) {
            var fraction = parts[1];
            if (fraction.length > decimals) {
                throw $.t("error_fraction_decimals", {
                    "decimals": decimals
                });
            } else if (fraction.length < decimals) {
                for (var i = fraction.length; i < decimals; i++) {
                    fraction += "0";
                }
            }
            qnt += fraction;
        } else {
            throw $.t("error_invalid_input");
        }

        //in case there's a comma or something else in there.. at this point there should only be numbers
        if (!/^\d+$/.test(qnt)) {
            throw $.t("error_invalid_input_numbers");
        }

        //remove leading zeroes
        return qnt.replace(/^0+/, "");
    };
    BRS.format = function(params, no_escaping) {
        if (typeof params !== "object") {
            var amount = String(params);
            var negative = amount.charAt(0) === "-" ? "-" : "";
            if (negative) {
                amount = amount.substring(1);
            }
            params = {
                "amount": amount,
                "negative": negative,
                "afterComma": ""
            };
        }

        var amount = String(params.amount);

        var digits = amount.split("").reverse();
        var formattedAmount = "";

        for (var i = 0; i < digits.length; i++) {
            if (i > 0 && i % 3 === 0) {
                formattedAmount = "'" + formattedAmount;
            }
            formattedAmount = digits[i] + formattedAmount;
        }

        var output = (params.negative ? params.negative : "") + formattedAmount + params.afterComma;

        if (!no_escaping) {
            output = output.escapeHTML();
        }

        return output;
    };

    BRS.formatQuantity = function(quantity, decimals, no_escaping) {
        return BRS.format(BRS.convertToQNTf(quantity, decimals, true), no_escaping);
    };

    BRS.formatAmount = function(amount, round, no_escaping) {
        if (typeof amount === "undefined") {
            return "0";
        } else if (typeof amount === "string") {
            amount = new BigInteger(amount);
        }

        var negative = "";
        var afterComma = "";

        if (typeof amount === "object") {
            var params = BRS.convertToNXT(amount, true);

            negative = params.negative;
            amount = params.amount;
            afterComma = params.afterComma;
        } else {
            //rounding only applies to non-nqt
            if (round) {
                amount = (Math.round(amount * 100) / 100);
            }

            if (amount < 0) {
                amount = Math.abs(amount);
                negative = "-";
            }

            amount = "" + amount;

            if (amount.indexOf(".") !== -1) {
                afterComma = amount.substr(amount.indexOf("."));
                amount = amount.replace(afterComma, "");
            } else {
                afterComma = "";
            }
        }

        return BRS.format({
            "negative": negative,
            "amount": amount,
            "afterComma": afterComma
        }, no_escaping);
    };
    BRS.formatTimestamp = function(timestamp, date_only) {
        if (typeof timestamp === "object") {
            date = timestamp;
        } else {
            date = new Date(Date.UTC(2014, 7, 11, 2, 0, 0, 0) + timestamp * 1000);
        }
        var date;
        if (!isNaN(date) && typeof(date.getFullYear) == 'function') {
            var d = date.getDate();
            var dd = d < 10 ? '0' + d : d;
            var M = date.getMonth() + 1;
            var MM = M < 10 ? '0' + M : M;
            var yyyy = date.getFullYear();
            var yy = new String(yyyy).substring(2);

            var format = LOCALE_DATE_FORMAT;

            var res = format
                .replace(/dd/g, dd)
                .replace(/d/g, d)
                .replace(/MM/g, MM)
                .replace(/M/g, M)
                .replace(/yyyy/g, yyyy)
                .replace(/yy/g, yy);

            if (!date_only) {
                var hours = date.getHours();
                var originalHours = hours;
                var minutes = date.getMinutes();
                var seconds = date.getSeconds();

                if (!BRS.settings["24_hour_format"]) {
                    hours = hours % 12;
                }
                if (hours < 10) {
                    hours = "0" + hours;
                }
                if (minutes < 10) {
                    minutes = "0" + minutes;
                }
                if (seconds < 10) {
                    seconds = "0" + seconds;
                }
                res += " " + hours + ":" + minutes + ":" + seconds;

                if (!BRS.settings["24_hour_format"]) {
                    res += " " + (originalHours > 12 ? "PM" : "AM");
                }
            }

            return res;
        } else {
            return date.toLocaleString();
        }
    };

    BRS.formatTime = function(timestamp) {
        var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + timestamp * 1000);

        if (!isNaN(date) && typeof(date.getFullYear) === 'function') {
            var res = "";

            var hours = date.getHours();
            var minutes = date.getMinutes();
            var seconds = date.getSeconds();

            if (hours < 10) {
                hours = "0" + hours;
            }
            if (minutes < 10) {
                minutes = "0" + minutes;
            }
            if (seconds < 10) {
                seconds = "0" + seconds;
            }
            res += " " + hours + ":" + minutes + ":" + seconds;

            return res;
        } else {
            return date.toLocaleString();
        }
    };

    BRS.isPrivateIP = function(ip) {
        if (!/^\d+\.\d+\.\d+\.\d+$/.test(ip)) {
            return false;
        }
        var parts = ip.split('.');
        if (parts[0] === '10' || parts[0] === '127' || (parts[0] === '172' && (parseInt(parts[1], 10) >= 16 && parseInt(parts[1], 10) <= 31)) || (parts[0] === '192' && parts[1] === '168')) {
            return true;
        }
        return false;
    };

    BRS.convertToHex16 = function(str) {
        var hex, i;
        var result = "";
        for (i = 0; i < str.length; i++) {
            hex = str.charCodeAt(i).toString(16);
            result += ("000" + hex).slice(-4);
        }

        return result;
    };

    BRS.convertFromHex16 = function(hex) {
        var j;
        var hexes = hex.match(/.{1,4}/g) || [];
        var back = "";
        for (j = 0; j < hexes.length; j++) {
            back += String.fromCharCode(parseInt(hexes[j], 16));
        }

        return back;
    };

    BRS.convertFromHex8 = function(hex) {
        hex = hex.toString(); //force conversion
        var str = '';
        for (var i = 0; i < hex.length; i += 2)
            str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
        return str;
    };

    BRS.convertToHex8 = function(str) {
        var hex = '';
        for (var i = 0; i < str.length; i++) {
            hex += '' + str.charCodeAt(i).toString(16);
        }
        return hex;
    };

    BRS.getFormData = function($form, unmodified) {
        var serialized = $form.serializeArray();
        var data = {};

        for (var s in serialized) {
            data[serialized[s].name] = serialized[s].value;
        }

        if (!unmodified) {
            delete data.request_type;
            delete data.converted_account_id;
            delete data.merchant_info;
        }

        return data;
    };

    BRS.convertNumericToRSAccountFormat = function(account) {
        if (/^BURST\-/i.test(account)) {
            return String(account).escapeHTML();
        } else {
            var address = new NxtAddress();

            if (address.set(account)) {
                return address.toString().escapeHTML();
            } else {
                return "";
            }
        }
    };

    BRS.getAccountLink = function(object, acc) {
        if (typeof object[acc + "RS"] == "undefined") {
            return "/";
        } else {
            return "<a href='#' data-user='" + String(object[acc + "RS"]).escapeHTML() + "' class='user-info'>" + BRS.getAccountTitle(object, acc) + "</a>";
        }
    };

    BRS.getAccountTitle = function(object, acc) {
        var type = typeof object;

        var formattedAcc = "";

        if (type === "string" || type == "number") {
            formattedAcc = object;
            object = null;
        } else {
            if (typeof object[acc + "RS"] == "undefined") {
                return "/";
            } else {
                formattedAcc = String(object[acc + "RS"]).escapeHTML();
            }
        }

        if (formattedAcc === BRS.account || formattedAcc == BRS.accountRS) {
            return $.t("you");
        } else if (formattedAcc in BRS.contacts) {
            return BRS.contacts[formattedAcc].name.escapeHTML();
        } else {
            return String(formattedAcc).escapeHTML();
        }
    };

    BRS.getAccountFormatted = function(object, acc) {
        var type = typeof object;

        if (type === "string" || type == "number") {
            return String(object).escapeHTML();
        } else {
            if (typeof object[acc + "RS"] == "undefined") {
                return "";
            } else {
                return String(object[acc + "RS"]).escapeHTML();
            }
        }
    };

    BRS.getClipboardText = function(type) {
        switch (type) {
            case "account_id":
                return BRS.account;
            case "account_rs":
                return BRS.accountRS;
            case "message_link":
                return document.URL.replace(/#.*$/, "") + "#message:" + BRS.account;
            case "send_link":
                return document.URL.replace(/#.*$/, "") + "#send:" + BRS.account;
            case "asset_id":
                return $("#asset_id").text();
            case "asset_link":
                return document.URL.replace(/#.*/, "") + "#asset:" + $("#asset_id").text();
            default:
                return "";
        }
    };

    BRS.setupClipboardFunctionality = function() {
        var elements = "#asset_id_dropdown .dropdown-menu a, #account_id_dropdown .dropdown-menu a";

        if (BRS.isLocalHost) {
            $("#account_id_dropdown li.remote_only, #asset_info_dropdown li.remote_only").remove();
        }

        var $el = $(elements);

        if (BRS.inApp) {
            $el.on("click", function() {
                parent.postMessage({
                    "type": "copy",
                    "text": BRS.getClipboardText($(this).data("type"))
                }, "*");

                $.notify($.t("success_clipboard_copy"), {
                    "type": "success"
                });
            });
        } else {
            var clipboard = new Clipboard('.copy_link', {
                text: function(trigger) {
                    return BRS.getClipboardText(trigger.getAttribute('data-type'));
                }
            });

            if ($el.hasClass("dropdown-toggle")) {
                $el.removeClass("dropdown-toggle").data("toggle", "");
                $el.parent().remove(".dropdown-menu");
            }

            clipboard.on('success', function(e) {
                $.notify($.t("success_clipboard_copy"), {
                    "type": "success"
                });
            });

            clipboard.on('error', function(e) {
                $("#account_id_dropdown .dropdown-menu, #asset_id_dropdown .dropdown-menu").remove();
                $("#account_id_dropdown, #asset_id").data("toggle", "");
                $.notify($.t("error_clipboard_copy"));
            });
        }
    };

    BRS.dataLoaded = function(data, noPageLoad) {
        var $el = $("#" + BRS.currentPage + "_contents");

        if ($el.length) {
            $el.empty().append(data);
        } else {
            $el = $("#" + BRS.currentPage + "_table");
            $el.find("tbody").empty().append(data);
        }

        BRS.dataLoadFinished($el);

        if (!noPageLoad) {
            BRS.pageLoaded();
        }
    };

    BRS.dataLoadFinished = function($el, fadeIn) {
        var $parent = $el.parent();

        if (fadeIn) {
            $parent.hide();
        }

        $parent.removeClass("data-loading");

        var extra = $parent.data("extra");

        var empty = false;

        if ($el.is("table")) {
            if ($el.find("tbody tr").length > 0) {
                $parent.removeClass("data-empty");
                if ($parent.data("no-padding")) {
                    $parent.parent().addClass("no-padding");
                }

                if (extra) {
                    $(extra).show();
                }
            } else {
                empty = true;
            }
        } else {
            if ($.trim($el.html()).length === 0) {
                empty = true;
            }
        }

        if (empty) {
            $parent.addClass("data-empty");
            if ($parent.data("no-padding")) {
                $parent.parent().removeClass("no-padding");
            }
            if (extra) {
                $(extra).hide();
            }
        } else {
            $parent.removeClass("data-empty");
        }

        if (fadeIn) {
            $parent.stop(true, true).fadeIn(400, function() {
                $parent.show();
            });
        }
    };

    BRS.createInfoTable = function(data, fixed) {
        var rows = "";

        /*
	  var keys = [];

	  if (Object.keys) {
	  keys = Object.keys(data);
	  }
          else {
	  for (var key in data) {
	  keys.push(key);
	  }
	  }

	  keys.sort(function(a, b) {
	  if (a < b) {
	  return -1;
	  }
          else if (a > b) {
	  return 1
	  }
          else {
	  return 0
	  }
	  });

	  for (var i = 0; i < keys.length; i++) {
	  var key = keys[i];
	*/

        for (var key in data) {
            var value = data[key];

            var match = key.match(/(.*)(NQT|QNT|RS)$/);
            var type = "";

            if (match && match[1]) {
                key = match[1];
                type = match[2];
            }

            key = key.replace(/\s+/g, "").replace(/([A-Z])/g, function($1) {
                return "_" + $1.toLowerCase();
            });

            //no need to mess with input, already done if Formatted is at end of key
            if (/_formatted_html$/i.test(key)) {
                key = key.replace("_formatted_html", "");
                value = String(value);
            } else if (/_formatted$/i.test(key)) {
                key = key.replace("_formatted", "");
                value = String(value).escapeHTML();
            } else if (key == "quantity" && $.isArray(value)) {
                if ($.isArray(value)) {
                    value = BRS.formatQuantity(value[0], value[1]);
                } else {
                    value = BRS.formatQuantity(value, 0);
                }
            } else if (key === "price" || key === "total" || key === "amount" || key === "fee" || key === "refund" || key === "discount") {
                value = BRS.formatAmount(new BigInteger(String(value))) + " BURST";
            } else if (key === "sender" || key === "recipient" || key === "account" || key === "seller" || key === "buyer") {
                value = "<a href='#' data-user='" + String(value).escapeHTML() + "'>" + BRS.getAccountTitle(value) + "</a>";
            } else {
                value = String(value).escapeHTML().nl2br();
            }

            rows += "<tr><td style='font-weight:bold;white-space:nowrap" + (fixed ? ";width:150px" : "") + "'>" + $.t(key).escapeHTML() + (type ? " " + type.escapeHTML() : "") + ":</td><td style='width:90%;word-break:break-all'>" + value + "</td></tr>";
        }

        return rows;
    };

    BRS.getSelectedText = function() {
        var t = "";
        if (window.getSelection) {
            t = window.getSelection().toString();
        } else if (document.getSelection) {
            t = document.getSelection().toString();
        } else if (document.selection) {
            t = document.selection.createRange().text;
        }
        return t;
    };

    BRS.formatStyledAmount = function(amount, round) {
        amount = BRS.formatAmount(amount, round);

        amount = amount.split(".");
        if (amount.length === 2) {
            amount = amount[0] + "<span style='font-size:12px'>." + amount[1] + "</span>";
        } else {
            amount = amount[0];
        }

        return amount;
    };

    BRS.getUnconfirmedTransactionFromCache = function(type, subtype, fields) {
        return BRS.getUnconfirmedTransactionsFromCache(type, subtype, fields, true);
    };

    BRS.getUnconfirmedTransactionsFromCache = function(type, subtype, fields, single) {
        if (!BRS.unconfirmedTransactions.length) {
            return false;
        }

        if (typeof type === "number") {
            type = [type];
        }

        if (typeof subtype === "number") {
            subtype = [subtype];
        }

        var unconfirmedTransactions = [];

        for (var i = 0; i < BRS.unconfirmedTransactions.length; i++) {
            var unconfirmedTransaction = BRS.unconfirmedTransactions[i];

            if (type.indexOf(unconfirmedTransaction.type) === -1 || subtype.indexOf(unconfirmedTransaction.subtype) == -1) {
                continue;
            }

            if (fields) {
                for (var key in fields) {
                    if (unconfirmedTransaction[key] === fields[key]) {
                        if (single) {
                            return BRS.completeUnconfirmedTransactionDetails(unconfirmedTransaction);
                        } else {
                            unconfirmedTransactions.push(unconfirmedTransaction);
                        }
                    }
                }
            } else {
                if (single) {
                    return BRS.completeUnconfirmedTransactionDetails(unconfirmedTransaction);
                } else {
                    unconfirmedTransactions.push(unconfirmedTransaction);
                }
            }
        }

        if (single || unconfirmedTransactions.length === 0) {
            return false;
        } else {
            $.each(unconfirmedTransactions, function(key, val) {
                unconfirmedTransactions[key] = BRS.completeUnconfirmedTransactionDetails(val);
            });

            return unconfirmedTransactions;
        }
    };

    BRS.completeUnconfirmedTransactionDetails = function(unconfirmedTransaction) {
        if (unconfirmedTransaction.type === 3 && unconfirmedTransaction.subtype === 4 && !unconfirmedTransaction.name) {
            BRS.sendRequest("getDGSGood", {
                "goods": unconfirmedTransaction.attachment.goods
            }, function(response) {
                unconfirmedTransaction.name = response.name;
                unconfirmedTransaction.buyer = unconfirmedTransaction.sender;
                unconfirmedTransaction.buyerRS = unconfirmedTransaction.senderRS;
                unconfirmedTransaction.seller = response.seller;
                unconfirmedTransaction.sellerRS = response.sellerRS;
            }, false);
        } else if (unconfirmedTransaction.type === 3 && unconfirmedTransaction.subtype === 0) {
            unconfirmedTransaction.goods = unconfirmedTransaction.transaction;
        }

        return unconfirmedTransaction;
    };

    BRS.hasTransactionUpdates = function(transactions) {
        return ((transactions && transactions.length) || BRS.unconfirmedTransactionsChange);
    };

    BRS.showMore = function($el) {
        if (!$el) {
            $el = $("#" + BRS.currentPage + "_contents");
            if (!$el.length) {
                $el = $("#" + BRS.currentPage + "_table");
            }
        }
        var adjustheight = 40;
        var moreText = "Show more...";
        var lessText = "Show less...";

        $el.find(".showmore > .moreblock").each(function() {
            if ($(this).height() > adjustheight) {
                $(this).css("height", adjustheight).css("overflow", "hidden");
                $(this).parent(".showmore").append(' <a href="#" class="adjust"></a>');
                $(this).parent(".showmore").find("a.adjust").text(moreText).click(function(e) {
                    e.preventDefault();

                    if ($(this).text() === moreText) {
                        $(this).parents("div:first").find(".moreblock").css('height', 'auto').css('overflow', 'visible');
                        $(this).parents("div:first").find("p.continued").css('display', 'none');
                        $(this).text(lessText);
                    } else {
                        $(this).parents("div:first").find(".moreblock").css('height', adjustheight).css('overflow', 'hidden');
                        $(this).parents("div:first").find("p.continued").css('display', 'block');
                        $(this).text(moreText);
                    }
                });
            }
        });
    };

    BRS.showFullDescription = function($el) {
        $el.addClass("open").removeClass("closed");
        $el.find(".description_toggle").text("Less...");
    };

    BRS.showPartialDescription = function($el) {
        if ($el.hasClass("open") || $el.height() > 40) {
            $el.addClass("closed").removeClass("open");
            $el.find(".description_toggle").text("More...");
        } else {
            $el.find(".description_toggle").text("");
        }
    };

    $("body").on(".description_toggle", "click", function(e) {
        e.preventDefault();

        if ($(this).closest(".description").hasClass("open")) {
            BRS.showPartialDescription();
        } else {
            BRS.showFullDescription();
        }
    });

    $("#offcanvas_toggle").on("click", function(e) {
        e.preventDefault();

        //If window is small enough, enable sidebar push menu
        if ($(window).width() <= 992) {
            $('.row-offcanvas').toggleClass('active');
            $('.left-side').removeClass("collapse-left");
            $(".right-side").removeClass("strech");
            $('.row-offcanvas').toggleClass("relative");
        } else {
            //Else, enable content streching
            $('.left-side').toggleClass("collapse-left");
            $(".right-side").toggleClass("strech");
        }
    });

    $.fn.tree = function() {
        return this.each(function() {
            var btn = $(this).children("a").first();
            var menu = $(this).children(".treeview-menu").first();
            var isActive = $(this).hasClass('active');

            //initialize already active menus
            if (isActive) {
                menu.show();
                btn.children(".fa-angle-right").first().removeClass("fa-angle-right").addClass("fa-angle-down");
            }
            //Slide open or close the menu on link click
            btn.click(function(e) {
                e.preventDefault();
                if (isActive) {
                    //Slide up to close menu
                    menu.slideUp();
                    isActive = false;
                    btn.children(".fa-angle-down").first().removeClass("fa-angle-down").addClass("fa-angle-right");
                    btn.parent("li").removeClass("active");
                } else {
                    //Slide down to open menu
                    menu.slideDown();
                    isActive = true;
                    btn.children(".fa-angle-right").first().removeClass("fa-angle-right").addClass("fa-angle-down");
                    btn.parent("li").addClass("active");
                }
            });
        });
    };

    BRS.setCookie = function(name, value, days) {
        var expires;

        if (days) {
            var date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires = "; expires=" + date.toGMTString();
        } else {
            expires = "";
        }
        document.cookie = escape(name) + "=" + escape(value) + expires + "; path=/";
    };

    BRS.getCookie = function(name) {
        var nameEQ = escape(name) + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ') c = c.substring(1, c.length);
            if (c.indexOf(nameEQ) === 0) return unescape(c.substring(nameEQ.length, c.length));
        }
        return null;
    };

    BRS.deleteCookie = function(name) {
        BRS.setCookie(name, "", -1);
    };

    BRS.translateServerError = function(response) {
        if (!response.errorDescription) {
            if (response.errorMessage) {
                response.errorDescription = response.errorMessage;
            } else if (response.error) {
                if (typeof response.error === "string") {
                    response.errorDescription = response.error;
                    response.errorCode = -1;
                } else {
                    return $.t("error_unknown");
                }
            } else {
                return $.t("error_unknown");
            }
        }

        switch (response.errorCode) {
            case -1:
                switch (response.errorDescription) {
                    case "Invalid ordinary payment":
                        return $.t("error_invalid_ordinary_payment");
                    case "Missing alias name":
                        return $.t("error_missing_alias_name");
                    case "Transferring aliases to Genesis account not allowed":
                        return $.t("error_alias_transfer_genesis");
                    case "Ask order already filled":
                        return $.t("error_ask_order_filled");
                    case "Bid order already filled":
                        return $.t("error_bid_order_filled");
                    case "Only text encrypted messages allowed":
                        return $.t("error_encrypted_text_messages_only");
                    case "Missing feedback message":
                        return $.t("error_missing_feedback_message");
                    case "Only text public messages allowed":
                        return $.t("error_public_text_messages_only");
                    case "Purchase does not exist yet or not yet delivered":
                        return $.t("error_purchase_delivery");
                    case "Purchase does not exist or is not delivered or is already refunded":
                        return $.t("error_purchase_refund");
                    case "Recipient account does not have a public key, must attach a public key announcement":
                        return $.t("error_recipient_no_public_key_announcement");
                    case "Transaction is not signed yet":
                        return $.t("error_transaction_not_signed");
                    case "Transaction already signed":
                        return $.t("error_transaction_already_signed");
                    case "PublicKeyAnnouncement cannot be attached to transactions with no recipient":
                        return $.t("error_public_key_announcement_no_recipient");
                    case "Announced public key does not match recipient accountId":
                        return $.t("error_public_key_different_account_id");
                    case "Public key for this account has already been announced":
                        return $.t("error_public_key_already_announced");
                    default:
                        if (response.errorDescription.indexOf("Alias already owned by another account") !== -1) {
                            return $.t("error_alias_owned_by_other_account");
                        } else if (response.errorDescription.indexOf("Invalid alias sell price") !== -1) {
                            return $.t("error_invalid_alias_sell_price");
                        } else if (response.errorDescription.indexOf("Alias hasn't been registered yet") !== -1) {
                            return $.t("error_alias_not_yet_registered");
                        } else if (response.errorDescription.indexOf("Alias doesn't belong to sender") !== -1) {
                            return $.t("error_alias_not_from_sender");
                        } else if (response.errorDescription.indexOf("Alias is owned by account other than recipient") !== -1) {
                            return $.t("error_alias_not_from_recipient");
                        } else if (response.errorDescription.indexOf("Alias is not for sale") !== -1) {
                            return $.t("error_alias_not_for_sale");
                        } else if (response.errorDescription.indexOf("Invalid alias name") !== -1) {
                            return $.t("error_invalid_alias_name");
                        } else if (response.errorDescription.indexOf("Invalid URI length") !== -1) {
                            return $.t("error_invalid_alias_uri_length");
                        } else if (response.errorDescription.indexOf("Invalid ask order") !== -1) {
                            return $.t("error_invalid_ask_order");
                        } else if (response.errorDescription.indexOf("Invalid bid order") !== -1) {
                            return $.t("error_invalid_bid_order");
                        } else if (response.errorDescription.indexOf("Goods price or quantity changed") !== -1) {
                            return $.t("error_dgs_price_quantity_changed");
                        } else if (response.errorDescription.indexOf("Invalid digital goods price change") !== -1) {
                            return $.t("error_invalid_dgs_price_change");
                        } else if (response.errorDescription.indexOf("Invalid digital goods refund") !== -1) {
                            return $.t("error_invalid_dgs_refund");
                        } else if (response.errorDescription.indexOf("Purchase does not exist yet, or already delivered") !== -1) {
                            return $.t("error_purchase_not_exist_or_delivered");
                        } else if (response.errorDescription.match(/Goods.*not yet listed or already delisted/)) {
                            return $.t("error_dgs_not_listed");
                        } else if (response.errorDescription.match(/Delivery deadline has already expired/)) {
                            return $.t("error_dgs_delivery_deadline_expired");
                        } else if (response.errorDescription.match(/Invalid effective balance leasing:.*recipient account.*not found or no public key published/)) {
                            return $.t("error_invalid_balance_leasing_no_public_key");
                        } else if (response.errorDescription.indexOf("Invalid effective balance leasing") != -1) {
                            return $.t("error_invalid_balance_leasing");
                        } else if (response.errorDescription.match(/Wrong buyer for.*expected:.*/)) {
                            return $.t("error_wrong_buyer_for_alias");
                        } else {
                            return response.errorDescription;
                        }

                        break;
                }
                var match; break;
            case 1:
                switch (response.errorDescription) {
                    case "This request is only accepted using POST!":
                        return $.t("error_post_only");
                    case "Incorrect request":
                        return $.t("error_incorrect_request");
                    default:
                        return response.errorDescription;
                }
                break;
            case 2:
                return response.errorDescription;
            case 3:
                match = response.errorDescription.match(/"([^"]+)" not specified/i);
                if (match && match[1]) {
                    return $.t("error_not_specified", {
                        "name": BRS.getTranslatedFieldName(match[1]).toLowerCase()
                    }).capitalize();
                }

                match = response.errorDescription.match(/At least one of (.*) must be specified/i);
                if (match && match[1]) {
                    var fieldNames = match[1].split(",");
                    var translatedFieldNames = [];

                    $.each(fieldNames, function(fieldName) {
                        translatedFieldNames.push(BRS.getTranslatedFieldName(fieldName).toLowerCase());
                    });

                    var translatedFieldNamesJoined = translatedFieldNames.join(", ");

                    return $.t("error_not_specified", {
                        "names": translatedFieldNamesJoined,
                        "count": translatedFieldNames.length
                    }).capitalize();
                } else {
                    return response.errorDescription;
                }
                break;
            case 4:
                match = response.errorDescription.match(/Incorrect "([^"]+)"/i);

                if (match && match[1]) {
                    return $.t("error_incorrect_name", {
                        "name": BRS.getTranslatedFieldName(match[1]).toLowerCase()
                    }).capitalize();
                } else {
                    return response.errorDescription;
                }
                break;
            case 5:
                match = response.errorDescription.match(/Unknown (.*)/i);
                if (match && match[1]) {
                    return $.t("error_unknown_name", {
                        "name": BRS.getTranslatedFieldName(match[1]).toLowerCase()
                    }).capitalize();
                }

                if (response.errorDescription === "Account is not forging") {
                    return $.t("error_not_forging");
                } else {
                    return response.errorDescription;
                }
                break;
            case 6:
                switch (response.errorDescription) {
                    case "Not enough assets":
                        return $.t("error_not_enough_assets");
                    case "Not enough funds":
                        return $.t("error_not_enough_funds");
                    default:
                        return response.errorDescription;
                }
                break;
            case 7:
                if (response.errorDescription === "Not allowed") {
                    return $.t("error_not_allowed");
                } else {
                    return response.errorDescription;
                }
                break;
            case 8:
                switch (response.errorDescription) {
                    case "Goods have not been delivered yet":
                        return $.t("error_goods_not_delivered_yet");
                    case "Feedback already sent":
                        return $.t("error_feedback_already_sent");
                    case "Refund already sent":
                        return $.t("error_refund_already_sent");
                    case "Purchase already delivered":
                        return $.t("error_purchase_already_delivered");
                    case "Decryption failed":
                        return $.t("error_decryption_failed");
                    case "No attached message found":
                        return $.t("error_no_attached_message");
                    case "recipient account does not have public key":
                        return $.t("error_recipient_no_public_key");
                    default:
                        return response.errorDescription;
                }
                break;
            case 9:
                if (response.errorDescription === "Feature not available") {
                    return $.t("error_feature_not_available");
                } else {
                    return response.errorDescription;
                }
                break;
            default:
                return response.errorDescription;
        }
    };

    BRS.getTranslatedFieldName = function(name) {
        var nameKey = String(name).replace(/NQT|QNT|RS$/, "").replace(/\s+/g, "").replace(/([A-Z])/g, function($1) {
            return "_" + $1.toLowerCase();
        });

        if (nameKey.charAt(0) === "_") {
            nameKey = nameKey.substring(1);
        }

        if ($.i18n.exists(nameKey)) {
            return $.t(nameKey).escapeHTML();
        } else {
            return nameKey.replace(/_/g, " ").escapeHTML();
        }
    };

    return BRS;
}(BRS || {}, jQuery));
