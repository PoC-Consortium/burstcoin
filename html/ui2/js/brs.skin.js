$(function() {
  var $theme = $('body').data('brs.theme')
  // Reinitialize variables on load
  $(window).on('load', function() {
    $theme = $('body').data('brs.theme')
  })
  var mySkins = [
    'skin-blue',
    'skin-red',
    'skin-yellow',
    'skin-purple',
    'skin-green',
  ]
  /**
   * get stored setting
   **/
  function get(name) {
    if (typeof(Storage) !== 'undefined') {
      return localStorage.getItem(name)
    }
  }
  /**
   * Store a new settings in the browser
   **/
  function store(name, val) {
    if (typeof(Storage) !== 'undefined') {
      localStorage.setItem(name, val)
    }
  }
  /**
   * Replaces the old skin with the new skin
   **/
  function changeSkin(cls) {
    $.each(mySkins, function(i) {
      $('body').removeClass(mySkins[i])
    })
    $('body').addClass(cls)
    store('skin', cls)
    return false
  }
  /**
   * Retrieve default settings and apply them to the template
   **/
  function setup() {
    var tmp = get('skin')
    if (tmp && $.inArray(tmp, mySkins))
      changeSkin(tmp)
    // Add the change skin listener
    $('[data-skin]').on('click', function(e) {
      if ($(this).hasClass('knob'))
        return
      e.preventDefault()
      changeSkin($(this).data('skin'))
    })
  }
  // Create the new tab
  var $tabPane = $('<div />', {
    'class': 'tab-pane active'
  })
  // Create the menu
  var $themeSettings = $('<div />')
  // theme options
  var $skinsList = $('<ul />', {
    'class': 'list-unstyled clearfix'
  })
  var $skinBlue =
    $('<li />', {
      style: 'float:left; width: 33.33333%; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-skin="skin-blue" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px; background: #367fa9"></span><span class="bg-light-blue" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>')
  $skinsList.append($skinBlue)
  var $skinPurple =
    $('<li />', {
      style: 'float:left; width: 33.33333%; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-skin="skin-purple" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-purple-active"></span><span class="bg-purple" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>')
  $skinsList.append($skinPurple)
  var $skinGreen =
    $('<li />', {
      style: 'float:left; width: 33.33333%; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-skin="skin-green" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-green-active"></span><span class="bg-green" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>')
  $skinsList.append($skinGreen)
  var $skinRed =
    $('<li />', {
      style: 'float:left; width: 33.33333%; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-skin="skin-red" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-red-active"></span><span class="bg-red" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>')
  $skinsList.append($skinRed)
  var $skinYellow =
    $('<li />', {
      style: 'float:left; width: 33.33333%; padding: 5px;'
    })
    .append('<a href="javascript:void(0)" data-skin="skin-yellow" style="display: block; box-shadow: 0 0 3px rgba(0,0,0,0.4)" class="clearfix full-opacity-hover">' +
      '<div><span style="display:block; width: 20%; float: left; height: 7px;" class="bg-yellow-active"></span><span class="bg-yellow" style="display:block; width: 80%; float: left; height: 7px;"></span></div>' +
      '<div><span style="display:block; width: 20%; float: left; height: 20px; background: #222d32"></span><span style="display:block; width: 80%; float: left; height: 20px; background: #f4f5f7"></span></div>' +
      '</a>')
  $skinsList.append($skinYellow)
  $themeSettings.append($skinsList)
  $tabPane.append($themeSettings)
  $('#themetab').after($tabPane)
  setup()
})