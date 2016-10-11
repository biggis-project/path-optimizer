// Adds an addOnClickListner method to the LeafletWidget.methods, 
// which allows to add an on click listner to an existing or 
// new added layer and triggers an Shiny.onInputChange when 
// an click is detected with the position clicked as lat lng value
LeafletWidget.methods.addOnClickListner = function(category, layerId, inputId) {
  // handler for the click event
  function onClickHandler(e) {
    let eventInfo = {
      id: layerId,
      '.nonce': Math.random(), // force reactivity
      lat: e.latlng.lat,
      lng: e.latlng.lng
    };
    console.log('clicked: ' + JSON.stringify(eventInfo));
    Shiny.onInputChange(inputId, eventInfo);
  }
  
  let this_ = this;
  // Add the on click listner to a new layer
  this.on('layeradd', function () {
    let layer = this_.layerManager.getLayer(category, layerId);
    if (typeof layer !== 'undefined' 
        && (!layer.hasEventListeners('click') 
          || (layer.hasEventListeners('click') 
            && layer._onMouseClick.name !== 'onClickHandler'))) {
      // console.log('add onclick listner to layer ' + layerId);
      layer.on('click', onClickHandler);
    }
  });
};

// handler for the marker click event
function markerClickHandler(e) {
  e.preventDefault();
  let id = e.currentTarget.id;
  let inputId = id.substr(0, id.lastIndexOf('_'));
  let rank = parseInt(id.substr(id.lastIndexOf('_') + 1));
  let eventInfo = {
    '.nonce': Math.random(), // force reactivity
    rank: rank,
  };
  Shiny.onInputChange(inputId, eventInfo);
}

// add an onclick listner to marker with the show-routes class
$(document).on('click', '.show-routes', markerClickHandler);

// Message handler to toggle the class of the select action button when clicked
Shiny.addCustomMessageHandler('routing_select_start',
  function(message) {
    if (message.clicked) {
        $('#routing_select_start').addClass('action-button-clicked');
    } else {
        $('#routing_select_start').removeClass('action-button-clicked');
    }
  }
);
Shiny.addCustomMessageHandler('routing_select_destination',
  function(message) {
    if (message.clicked) {
        $('#routing_select_destination').addClass('action-button-clicked');
    } else {
        $('#routing_select_destination').removeClass('action-button-clicked');
    }
  }
);
Shiny.addCustomMessageHandler('optimaltime_select_start',
  function(message) {
    if (message.clicked) {
        $('#optimaltime_select_start').addClass('action-button-clicked');
    } else {
        $('#optimaltime_select_start').removeClass('action-button-clicked');
    }
  }
);


// Helper method to stringify JavaScript objects that contains cyclic references
// http://stackoverflow.com/questions/9382167/serializing-object-that-contains-cyclic-object-value/26816683#26816683
function JsonStringifyCyclic(obj) {
  var seen = [];
  let json = JSON.stringify(obj, function (key, val) {
    if (val !== null && typeof val === 'object') {
      if (seen.indexOf(val) >= 0) {
        return;
      }
      seen.push(val);
    }
    return val;
  });
  return json;
}