$(function() {
  var $theme = $('body').data('brs.theme');
  // Reinitialize variables on load
  $(window).on('load', function() {
    $theme = $('body').data('brs.theme');
  });
  var mythemes = [
    'theme-blue',
    'theme-red',
    'theme-yellow',
    'theme-purple',
    'theme-green',
  ];
  /**
   * get stored setting
   **/
  function get(name) {
    if (typeof(Storage) !== 'undefined') {
      return localStorage.getItem(name);
    }
  }
  /**
   * Store a new settings in the browser
   **/
  function store(name, val) {
    if (typeof(Storage) !== 'undefined') {
      localStorage.setItem(name, val);
    }
  }
  /**
   * Replaces the old theme with the new theme
   **/
  function changetheme(cls) {
    $.each(mythemes, function(i) {
      $('body').removeClass(mythemes[i]);
    });
    $('body').addClass(cls);
    store('theme', cls);
    return false;
  }
  /**
   * Retrieve default settings and apply them to the template
   **/
  function setup() {
    var tmp = get('theme');
    if (tmp && $.inArray(tmp, mythemes))
      changetheme(tmp);
    // Add the change theme listener
    $('[data-theme]').on('click', function(e) {
      if ($(this).hasClass('knob'))
        return;
      e.preventDefault();
      changetheme($(this).data('theme'));
    });
  }
  // Create the new tab
  var $themesettings = $('<div />', {
    'class': 'theme-settings active'
  });
  // Create the menu
  var $themeSettings = $('<div />');
  // theme options
  var $themesList = $('<ul />', {
    'class': 'list-unstyled clearfix'
  });
  var $themeBlue =
    $('<li />', {
      style: 'float:left; width: 150px; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-theme="theme-blue" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px; background: #367fa9"></span><span class="bg-light-blue" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>');
  $themesList.append($themeBlue);
  var $themePurple =
    $('<li />', {
      style: 'float:left; width: 150px; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-theme="theme-purple" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-purple-active"></span><span class="bg-purple" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>');
  $themesList.append($themePurple);
  var $themeGreen =
    $('<li />', {
      style: 'float:left; width: 150px; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-theme="theme-green" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-green-active"></span><span class="bg-green" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>');
  $themesList.append($themeGreen);
  var $themeRed =
    $('<li />', {
      style: 'float:left; width: 150px; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-theme="theme-red" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-red-active"></span><span class="bg-red" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>');
  $themesList.append($themeRed);
  var $themeYellow =
    $('<li />', {
      style: 'float:left; width: 150px; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-theme="theme-yellow" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-yellow-active"></span><span class="bg-yellow" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>');
  $themesList.append($themeYellow);
  $themeSettings.append($themesList);
  $themesettings.append($themeSettings);
  $('#themebox').after($themesettings);
  setup();
});