
$(document).ready(function() {
   $('#start').click(startButtonClick);
   $('#leaveButton').click(leaveButtonClick);
   $("#errorDialog").dialog({ autoOpen: false, height:'auto', width:'auto'});
   $("#dismissErrorButton").click(dismissErrorButtonHandler);
   $("#areYouSureDialog").dialog({ autoOpen: false });

   $('#databaseActionPaneButton').click(databaseActionPaneButtonClick);
   $('#databaseResetButton').click(resetDatabase);

   $('#xddValidatorActionPaneButton').click(xddValidatorActionPaneButtonClick);

   $('#dataExplorerActionPaneButton').click(dataExplorerActionPaneButtonClick);
   $(".treeButton").click(treeButtonHandler);
   $(".editButton").click(editButtonHandler);
   $("#editDialog").dialog({ autoOpen: false, width:'auto' });
   $("#copyButton").click(copyButtonHandler);
   $("#cutButton").click(cutButtonHandler);
   $("#deleteButton").click(deleteButtonHandler);
   $("#pasteOntoButton").click(pasteOntoButtonHandler);
   $("#pasteUnderButton").click(pasteUnderButtonHandler);

   $('#clipboardActionPaneButton').click(clipboardActionPaneButtonClick);
   $('#clipboard').on('keyup',clipboardOnKeyUp);
   $('#clipboardDuplicateButton').click(clipboardDuplicateButtonHandler);
   $('#clipboardHistorySelect').change(clipboardHistorySelectChangeHandler);

   $('#csmlValidatorActionPaneButton').click(csmlValidatorActionPaneButtonClick);
   $('#csmlValidateButton').click(csmlValidateButtonClick);
   $('#csml').on('keyup',csmlOnKeyUp);

   $("#createChildDialog").dialog({ autoOpen: false, width:'auto' });
   $("#createChildDialogButton").click(createChildDialogButtonHandler);
   $("#createChildButton").click(createChildButtonHandler);
   $('#createChildBaseTypeSelect').change(createChildBaseTypeSelectChangeHandler);

   $("#createMetadataDialog").dialog({ autoOpen: false, width:'auto' });
   $("#createMetadataDialogButton").click(createMetadataDialogButtonHandler);
   $("#createMetadataButton").click(createMetadataButtonHandler);
   $("#createMetadataName").change(createMetadataNameChangeHandler);

   $("#changeValueDialog").dialog({ autoOpen: false, width:'auto' });
   $("#changeValueDialogButton").click(changeValueDialogButtonHandler);
   $("#changeValueButton").click(changeValueButtonHandler);
   $("#changeValueHistory").change(changeValueHistoryChangeHandler);

   $("#createInstanceDialog").dialog({ autoOpen: false, width: 'auto' });
   $("#createInstanceDialogButton").click(createInstanceDialogButtonHandler);
   $("#createInstanceButton").click(createInstanceButtonHandler);
   $("#createInstanceMemberMinInput").change(createInstanceMemberMinInputHandler);
   $("#createInstanceMemberMaxInput").change(createInstanceMemberMaxInputHandler);
   $("#createInstanceOptionalPercentInput").change(createInstanceMemberMaxInput);
   $("#createInstanceDepthLimitInput").change(rangedNumericInputChangeHandler);
   $("#createInstanceCountLimitInput").change(rangedNumericInputChangeHandler);

   $("#createSpecialDialog").dialog({ autoOpen: false, width: 'auto' });
   $("#createSpecialDialogButton").click(createSpecialDialogButtonHandler);
   $("#createCSMLButton").click(createCSMLButtonHandler);
   $("#createDefinitionsButton").click(createDefinitionsButtonHandler);
   $("#createTagDefinitionsButton").click(createTagDefinitionsButtonHandler);

   /*
   $('#pathExplorerActionPaneButton').click(pathExplorerActionPaneButtonClick);
   $('#pathGoButton').click(pathGoButtonClick);
   $('#method').change(methodChange);
   $('#data').on('keyup',dataOnKeyUp);
   */

   serverRoot = new Node("");
   dataRoot = new Node("bws");
   serverRoot.add(dataRoot);

   initialize();

});

function closeDialogs() {
    $("#errorDialog").dialog("close");
    $("#editDialog").dialog("close");
    $("#createChildDialog").dialog("close");
    $("#createMetadataDialog").dialog("close");
    $("#createInstanceDialog").dialog("close");
    $("#createDefinitionDialog").dialog("close");
    $("#createSpecialDialog").dialog("close");
    $("#changeValueDialog").dialog("close");
    $("#areYouSureDialog").dialog("close");
}

function genericErrorHandler(xhr, textStatus, errorThrown) {
   var resultHtml = '<p>HTTP Status: ' + xhr.status + ' ' + errorThrown + '</p><p>Response body:</p><div><pre>'+xhr.responseText + '</pre></div>';
   $('#result').html(resultHtml);
   $('#errorDialogText').html(resultHtml);
   $("#errorDialog").dialog("open");
}

function dismissErrorButtonHandler() {
   $("#errorDialog").dialog("close");
}

function showModeSection(sectionName) {
   // there must be an cleaner way to do this!
   $('#greeting').hide();
   $('#inactiveSession').hide();
   $('#activeSessionYou').hide();
   $('#activeSessionOther').hide();
   $(sectionName).show();
}

function showActionSection(sectionName) {
   // there must be an cleaner way to do this!
   $('#databaseActionPane').hide();
   $('#dataBuilderActionPane').hide();
   $('#csmlValidatorActionPane').hide();
   $('#xddValidatorActionPane').hide();
   $('#dataExplorerActionPane').hide();
   //$('#pathExplorerActionPane').hide();
   $('#clipboardActionPane').hide();

   $('#csmlValidatorActionPaneButton').css('background-color', 'white');
   $('#xddValidatorActionPaneButton').css('background-color', 'white');
   $('#databaseActionPaneButton').css('background-color', 'white');
   $('#dataBuilderActionPaneButton').css('background-color', 'white');
   $('#dataExplorerActionPaneButton').css('background-color', 'white');
   //$('#pathExplorerActionPaneButton').css('background-color', 'white');
   $('#clipboardActionPaneButton').css('background-color', 'white');
   $(sectionName).show();
   $(sectionName+"Button").css('background-color', 'gray');
}

function setResult(something) {
   $('#result').html(something);
}

function addResult(something) {
   $("#result").append(something);
}

function initialize() {    // initialize the page by checking for an active session
    $.post("/ui/rpc?"+$.param({op:"checkSession"}))
       .fail(genericErrorHandler)
       .done(function(data, textStatus, jqXHR) {
           if (data.own === "none") {  // it's available
              setResult('<p>Session available</p>');
              showModeSection('#inactiveSession');
           }
           else if (data.own === "you") {  // it's yours
              setResult('<p>Session reclaimed</p>');
              $('#activeYouExp').html(data.exp);
              $('#locale').val(data.loc);
              showModeSection('#activeSessionYou');
              showActionSection("none");
              getParameters();
           }
           else if (data.own === "other") {  // it's someone else's
              setResult('<p>Session is owned by another user</p>');
              $('#activeOtherSid').html(data.sid);
              $('#activeOtherExp').html(data.exp);
              showModeSection('#activeSessionOther');
           }
           else {
              setResult('<p>Internal Error! (data.sid='+data.sid+')</p>');
           }

       });
}

g_maxPopulateDepth = 50;   // overridden by getParameters()
g_maxPopulateCount = 1000; // overridden by getParameters()

function getParameters() {
   $.post("/ui/rpc?"+$.param({op:"getParameters"}))
      .fail(genericErrorHandler)
      .done(function(data, textStatus, jqXHR) {
         setResult('');
         g_maxPopulateCount = data.maxPopulateCount;
         g_maxPopulateDepth = data.maxPopulateDepth;
         $("#createInstanceDepthLimitInput").attr("max",g_maxPopulateDepth);
         $("#createInstanceCountLimitInput").attr("max",g_maxPopulateCount);
      });
}

function startButtonClick() {
    $.post("/ui/rpc?"+$.param({ op:"startSession", nick:$('#nick').val() }))
        .fail(genericErrorHandler)
        .done(function(data, textStatus, jqXHR) {
            setResult('<p>Session started</p>');
            $('#activeYouExp').html(data.exp);
            showModeSection('#activeSessionYou');
            showActionSection("none");
            getParameters();
        });
}

function leaveButtonClick() {
    $.post("/ui/rpc?"+$.param({ op:"endSession", sid:$('#sid').val() }))
        .fail(genericErrorHandler)
        .done();
    initialize();
}

////////////////////////////////////////////////////////////////////////
/////////////////////// CSML VALIDATOR TAB /////////////////////////////
////////////////////////////////////////////////////////////////////////

function csmlValidatorActionPaneButtonClick() {
    setResult("");
    showActionSection('#csmlValidatorActionPane');
}

function csmlValidateButtonClick() {
    setResult("Pending...");
    var cfg;
    if ($('#validateWithEmptyRadio').is(":checked"))       cfg = "config-empty";
    else if ($('#validateWithDefsRadio').is(":checked"))   cfg = "config-with-defs";
    else if ($('#validateInCurrentRadio').is(":checked"))  cfg = "current";
    else if ($('#validateAndPersistRadio').is(":checked")) cfg = "current-persist";
    else cfg = "???"
    $.ajax({
        url: "/ui/rpc?"+$.param({
            op:"csmlValidate",
            cfg:cfg,
            path:$('#validatePersistPath').val()
        }),
        dataType: "json", // response data type
        contentType: "application/xml; charset=utf-8",
        type: "POST",
        data: $('#csml').val(),
        error: genericErrorHandler,
        success: function(data, textStatus, jqXHR) {
           setResult("<pre>"+escapeHtml(data.err)+"</pre>");
        }
    });
}

function csmlOnKeyUp() {  // autosize the csml textarea
   $(this).height( 0 );
   $(this).height( this.scrollHeight );
}

////////////////////////////////////////////////////////////////////////
/////////////////////////// DATABASE TAB ///////////////////////////////
////////////////////////////////////////////////////////////////////////

function databaseActionPaneButtonClick() {
    setResult("");
    showActionSection('#databaseActionPane');
}

function dataBuilderActionPaneButtonClick() {
    setResult("");
    showActionSection('#dataBuilderActionPane');
}

function getResetConfig() {
    if ($('#makeEmptyRadio').is(":checked"))    return "config-empty";
    if ($('#makeDefsRadio').is(":checked"))     return "config-with-defs";
    if ($('#makeExamplesRadio').is(":checked")) return "config-with-examples";
    return "config-empty";
}

function resetDatabase() {
    $.post("/ui/rpc?"+$.param({ op:"resetDatabase", loc:$('#locale').val(), cfg:getResetConfig() }))
        .fail(genericErrorHandler)
        .done(function(data, textStatus, jqXHR) {
            setResult("<pre>"+escapeHtml(data.err)+"</pre>");
            dataRoot.collapse(); // collapse "data explorer" tree since it's probably invalid now
        });
}

////////////////////////////////////////////////////////////////////////
//////////////////////// XDD VALIDATOR TAB /////////////////////////////
////////////////////////////////////////////////////////////////////////


function xddValidatorActionPaneButtonClick() {
    setResult("");
    showActionSection('#xddValidatorActionPane');
}

////////////////////////////////////////////////////////////////////////
//////////////////////// DATA EXPLORER TAB /////////////////////////////
////////////////////////////////////////////////////////////////////////

function dataExplorerActionPaneButtonClick() {
    setResult("");
    showActionSection('#dataExplorerActionPane');
}

function Node(name) {  // Node class holds the value for the tree nodes
   this.parent = null;
   this.name   = name;
   this.subs = {};    // children and metadata
   this.baseType = null;
   this.nodeType = null;
   this.displayName = null;
   this.value = null;
   this.treeButtonId = null;
   this.writable = null;     // for when it's locally marked as writable. use isWritable() to test up the tree

   this.getPath = function() {
      result = name;
      node = this;
      while (node.parent != null) {
         node = node.parent;
         result = node.name + "/" + result;
      }
      return result;
   }
   this.isWritable = function() {
      if (this.writable != null) return this.writable;
      if (this.parent != null) return this.parent.isWritable();
      return false;
   }
   this.canHaveChildren = function() {
      return canHaveChildren(this.baseType);
   }
   this.canHaveValue = function() {
      return canHaveValue(this.baseType);
   }
   this.setInfoFrom = function (data) {
      if (typeof data === 'object') { // data item is in JSON object form, so there might be extra info available for it
          if ("$base" in data)        this.baseType = data.$base;
          if ("$nodeType" in data)    this.nodeType = data.$nodeType;
          if ("$value" in data)       this.value    = data.$value;
          if ("$writable" in data)    this.writable = data.$writable;
          if ("$displayName" in data) this.displayName = (typeof data.$displayName === 'object')? data.$displayName.$value : data.$displayName;
      }
      else { // data item is in JSON primitive form, so nothing but value is available
          this.value = data;
      }
   }
   this.makeInfoHTML = function() {
      var name = this.name;
      if      (name.startsWith("$org.ampii.ui.definitions"))    name = "<i>DEFINITIONS</i>";
      else if (name.startsWith("$org.ampii.ui.tagDefinitions")) name = "<i>TAGDEFINITIONS</i>";
      else if (name.startsWith(".csml"))                        name = "<i>CSML</i>";
      return name + " " +
             (this.nodeType != null? " "+ getNodeTypeIcon(this.nodeType)+" " : "") +
             (this.displayName != null? " <b>\""+this.displayName+"\"</b> " : "")  +
             "<span style=\"color:#808080\">" +
                (this.baseType != null? this.baseType : "") +
                (this.value != null? " = <i>"+this.value+"</i>" : "") +"&nbsp;"+
             "</span>";
   }
   this.add = function(sub) {
      // add JS object to our list of subs
      sub.parent = this;
      this.subs[sub.name] = sub;
      // then make new HTML <li> for the sub
      var subPath         = sub.getPath();
      var subHexPath      = hexEncode(subPath)
      var subNodeId       = "node"   + subHexPath;
      var subTreeButtonId = "button" + subHexPath;
      var subEditButtonId = "edit"   + subHexPath;
      var subSubsId       = "subs"   + subHexPath;
      var subInfoId       = "info"   + subHexPath;
      var subInfo         = sub.makeInfoHTML();
      var subHtml =
          "<li class=\"tree\" id="+subNodeId+">"+
             "<button id=\""+subTreeButtonId+"\" class=\"treeButton\" name=\""+subPath+"\">+</button>"+
             "<span id=\""+subInfoId+"\">"+subInfo+"</span>"+ // this info span can get replaced by refresh()
             (sub.doesNotHaveEditButton()? "" : "<button id=\""+subEditButtonId+"\" class=\"editButton\" name=\""+subPath+"\">&#9998;</button>")+
             "<ol id=\""+subSubsId+"\"></ol>" +  // this is empty/invisible by default
          "</li>";
      // now append that sub HTML to our HTML
      var ourSubsId = "#subs"+hexEncode(this.getPath());
      var ourSubs = $(ourSubsId);  // find our subs <ol> based on our path
      ourSubs.append(subHtml);
      // and bind this node to the tree buttons
      $("#"+subTreeButtonId).click(treeButtonHandler);
      $("#"+subTreeButtonId).data("node",sub);      // associate the JS object with the button
      if (!sub.doesNotHaveEditButton()) {
         $("#"+subEditButtonId).click(editButtonHandler);
         $("#"+subEditButtonId).data("node",sub);
      }
   }
   this.remove = function(sub) {
      // first remove the HTML for sub
      $("#node"+hexEncode(sub.getPath())).remove();
      // then remove the JS object from our list of subs
      sub.parent = null;
      delete this.subs[sub.name];
   }
   this.removeAll = function() {
      for (var subName in this.subs)
         this.remove(this.subs[subName]);  // TODO check JS doesn't have a problem with concurrent modification like Java?
   }
   this.collapse = function() {
      setResult("OK");
      this.removeAll();
      $("#button"+hexEncode(this.getPath())).html("+");
   }
   this.expand = function() {
      var path = this.getPath();
      var me   = this;

      $.get(path+"?metadata=base,value,nodeType,displayName,writable,"+$('#metadataFilter').val()+"&depth=1")
          .fail(genericErrorHandler)
          .done(function(data, textStatus, jqXHR) {
              setResult("OK");
              var checkExists = [];
              if (me.baseType === 'Link') data.$target="...";   // this injects "$target" in list of subs for nodes of type "Link"
              me.setInfoFrom(data);
              $("#info"+hexEncode(me.getPath())).html(me.makeInfoHTML());
              $.each(data, function(subName,subData){
                  // skip members already handled by setInfoFrom()
                  if (subName === "$$defaultLocale" || subName==="$partial" || subName==="$self" || subName==="$truncated" || subName==="$name" || subName==="$base" || subName==="$value") return true;
                  // the following are returned on the top node even if it is not locally present, so we need to confirm the existence before showing it in the tree
                  if (subName === "$authWrite" || subName === "$authVisible" || subName === "$readable" || subName === "$writable" || subName === "$variability" ||
                      subName === "$volatility" || subName === "$error" || subName === "$errorText" || subName === "$published" ||  subName === "$updated" || subName === "$author") {
                      checkExists.push(subName);
                      return true; // we will check to see if these really exist later (below)
                  }
                  // for all the rest... add as child node in the tree
                  // if we encounter a definitions context, make it into metadata so we can deal with it as normal data (sort of)
                  if (subName === "$$definitions")    subName = "$org.ampii.ui.definitions";
                  if (subName === "$$tagDefinitions") subName = "$org.ampii.ui.tagDefinitions";
                  var sub = new Node(subName);
                  sub.setInfoFrom(subData);
                  me.add(sub);
              });
              $.each(me.subs, function(subName,subData){
                  subData.refreshInfo(); // this gets metadata for metadata (like baseType) that was not reported even with metadata=all
              });
              $.each(checkExists, function(index,name){ // OK, now follow up on all the tree-inherited stuff to see if it really exists at this level or was just inherited
                  $.get(path+"/exists("+name+")")
                      .fail(genericErrorHandler)
                      .done(function(data, textStatus, jqXHR) {
                          setResult("OK");
                          if (data.$value) {  // yay, it exists. so NOW we can make a tree node and show it to the user
                             var sub = new Node(name);
                             me.add(sub);
                             sub.refreshInfo();
                          }
                      });
              });
      });
      $("#button"+hexEncode(this.getPath())).html("-");
   }
   this.refresh = function() {
      this.collapse();
      this.expand();
   }
   this.refreshInfo = function() {
      var path = this.getPath();
      var me   = this;
      if (this.name === "$target") {
         var pathWithoutTarget = path.substr(0, path.length-8);
         $.get(pathWithoutTarget+"/exists($target%252F$writable)")
            .fail(genericErrorHandler)
            .done(function(data, textStatus, jqXHR) {
               setResult("OK");
               if (data.$value) { // if $target/$writable exists
                  $.get(path+"/$writable")
                     .fail(genericErrorHandler)
                     .done(function(data, textStatus, jqXHR) {
                         setResult("OK");
                         me.writable = data.$value;

                     });
               }
               me.writable = false; // if $target/writable does not exist, then assume target is invalid or not writable
            });
      }
      else {
         $.get(path+"?metadata=base,value,nodeType,displayName&depth=0")
             .fail(genericErrorHandler)
             .done(function(data, textStatus, jqXHR) {
                 setResult("OK");
                 me.setInfoFrom(data);
                 $("#info"+hexEncode(me.getPath())).html(me.makeInfoHTML());
             });
      }
   }
   this.doesNotHaveInfo = function() {
      return this.name === "$target" || this.name === "$self";
   }
   this.doesNotHaveEditButton = function() {
      return this.name === "$target";
   }
}

function treeButtonHandler() {
   closeDialogs();
   var button = $(this);
   var node = button.data("node"); // get the JS Node associated with this button
   if (button.text() === "-") node.collapse();
   else node.expand();
}

nodeTypeBadges = {
   'tree':"[Tree]",'collection':"[Coll]",'area':"[Area]",'building':"[Bldg]",'floor':"[Flr]",'unknown':"[Unkn]",'system':"[Sys]",
   'network':"[Net]",'device':"[Dev]",'organizational':"[Org]",'equipment':"[Equ]",'point':"[Pnt]",'property':"[Prop]",
   'functional':"[Fun]",'other':"[Othr]",'object':"[Obj]",'subsystem':"[Sub]",'section':"[Sect]",'module':"[Mod]",'room':"[Room]",
   'zone':"[Zone]",'protocol':"[Prot]",'member':"[Memb]",'invalid':"[???]"
}

canHaveValue = function(baseType) {  return $.inArray(baseType, baseTypeCanHaveValue) > -1; }

baseTypeCanHaveValue = [
   "Poly","Boolean","Unsigned","Integer","Real","Double","OctetString","String","BitString","Enumerated","Date","DatePattern",
   "DateTime","DateTimePattern","Time","TimePattern","ObjectIdentifier","ObjectIdentifierPattern","WeekNDay","Link","StringSet","Raw"
]

canHaveChildren = function(baseType) {  return $.inArray(baseType, baseTypeCanHaveChildren) > -1; }

baseTypeCanHaveChildren = [
  "Array","List","Sequence","SequenceOf","Choice","Object","Composition","Collection","Unknown"
]

baseTypeOfMetadata = function(name) {
   var theList = {
      "$comment":"String",
      "$description":"String",
      "$displayName":"String",
      "$extends":"String",
      "$tags":"String",
      "$targetType":"String",
      "$type":"String",
      "$valueTags":"Collection",
      "$writable":"Boolean",
      "$bit":"Bit",
      "$choices":"Collection",
      "$length":"Unsigned",
      "$maximum":"Poly",
      "$maximumLength":"Unsigned",
      "$maximumSize":"Unsigned",
      "$memberType":"String",
      "$minimum":"Poly",
      "$minimumLength":"Unsigned",
      "$minimumSize":"Unsigned",
      "$namedBits":"Collection",
      "$namedValues":"Collection",
      "$units":"Enumerated",
      "$variability":"Enumerated",
      "$volatility":"Enumerated",
      "$absent":"Boolean",
      "$addRev":"String",
      "$allowedChoices":"StringSet",
      "$allowedTypes":"StringSet",
      "$alternate":"Link",
      "$associatedWith":"String",
      "$author":"String",
      "$authRead":"String",
      "$authVisible":"String",
      "$authWrite":"String",
      "$commandable":"Boolean",
      "$dataRev":"String",
      "$displayNameForWriting":"String",
      "$displayOrder":"Unsigned",
      "$documentation":"String",
      "$edit":"Link",
      "$fault":"Boolean",
      "$href":"Link",
      "$id":"String",
      "$inAlarm":"Boolean",
      "$isMultiline":"Boolean",
      "$links":"Collection",
      "$maximumEncodedLength":"Unsigned",
      "$maximumEncodedLengthForWriting":"Unsigned",
      "$maximumForWriting":"Unsigned",
      "$maximumLengthForWriting":"Unsigned",
      "$mediaType":"String",
      "$memberTypeDefinition":"List",
      "$minimumEncodedLength":"Unsigned",
      "$minimumEncodedLengthForWriting":"Unsigned",
      "$minimumForWriting":"Poly",
      "$minimumLengthForWriting":"Unsigned",
      "$modRev":"String",
      "$nodeType":"Enumerated",
      "$nodeSubtype":"String",
      "$notForReading":"Boolean",
      "$notForWriting":"Boolean",
      "$notPresentWith":"String",
      "$objectType":"String",
      "$optional":"Boolean",
      "$outOfService":"Boolean",
      "$overlays":"String",
      "$overridden":"Boolean",
      "$physical":"Link",
      "$priorityArray":"Array",
      "$propertyIdentifier":"Unsigned",
      "$published":"DateTime",
      "$readable":"Boolean",
      "$rel":"Link",
      "$related":"Link",
      "$relinquishDefault":"Poly",
      "$remRev":"String",
      "$represents":"Link",
      "$requiredWhen":"Enumerated",
      "$requiredWhenText":"String",
      "$requiredWith":"String",
      "$requiredWithout":"String",
      "$resolution":"Poly",
      "$revisions":"Collection",
      "$sourceId":"String",
      "$unitsText":"String",
      "$unspecifiedValue":"Boolean",
      "$updated":"DateTime",
      "$via":"Link",
      "$viaExternal":"Link",
      "$viaMap":"Link",
      "$writableWhen":"Enumerated",
      "$writableWhenText":"String",
      "$writeEffective":"Enumerated",
   }
   if (name in theList) { return theList[name]; }
   else return "";
}

function getNodeTypeIcon(nodeType) { // not really "icons" :-)
     return nodeType in nodeTypeBadges? nodeTypeBadges[nodeType] : nodeTypeBadges['invalid'];
}

////////////////////////////////////////////////////////////////////////
////////////////////// EDIT (PENCIL) DIALOG  ///////////////////////////
////////////////////////////////////////////////////////////////////////

function editButtonHandler() {
    var button = $(this);
    var node = button.data("node"); // find the JS Node object associated with this button
    g_selectedNode = node; // set a global var, yuck //TODO find better way to connect to later button handlers
    var writable        = node.isWritable();
    var parentWritable  = node.parent.isWritable();
    var canHaveChildren = node.canHaveChildren();
    var canHaveValue    = node.canHaveValue();
    // enable and disable buttons as appropriate.
    $("#cutButton").prop('disabled',!parentWritable);
    $("#deleteButton").prop('disabled',!parentWritable);
    $("#pasteOntoButton").prop('disabled',!writable);
    $("#pasteUnderButton").prop('disabled',!writable || !canHaveChildren);
    $("#createMetadataDialogButton").prop('disabled',!writable);
    $("#createChildDialogButton").prop('disabled',!writable || !canHaveChildren);
    $("#createInstanceDialogButton").prop('disabled',!writable || !canHaveChildren);
    $("#createSpecialDialogButton").prop('disabled',!writable || !canHaveChildren);
    $("#changeValueDialogButton").prop('disabled',!writable || !canHaveValue);
    $("#editDialog").dialog("open"); // present all the "edit" action buttons
}

var g_selectedNode;


////////////////////////////////////////////////////////////////////////
/////////////////////////// NEW METADATA ///////////////////////////////
////////////////////////////////////////////////////////////////////////

function createMetadataDialogButtonHandler() {
   closeDialogs();
   createMetadataDialogStyleUpdate();
   $("#createMetadataDialog").dialog("open");
}

function createMetadataNameChangeHandler() {
   createMetadataDialogStyleUpdate();
}

function createMetadataDialogStyleUpdate() {
   var baseType = baseTypeOfMetadata($("#createMetadataName").val());
   var chv = canHaveValue(baseType);
   $("#createMetadataValueSpan").css("color",chv?"#000000":"#808080");
   $("#createMetadataValue").css("color",chv?"#000000":"#808080");
   $("#createMetadataValue").prop('disabled',!chv);
}

function createMetadataButtonHandler() {
    var node  = g_selectedNode;
    var name  = $("#createMetadataName").val();
    var value = $("#createMetadataValue").val();
    var path  = node.getPath()+"/"+name;
    $.ajax({
        url: path,
        type: 'PUT',
        contentType: "application/json",
        data: "{\"$value\":\""+value+"\"}",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           node.refresh();// these are async, so do refresh only after PUT completes
        }
    });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
///////////////////////////// NEW CHILD ////////////////////////////////
////////////////////////////////////////////////////////////////////////

function createChildDialogButtonHandler() {
   closeDialogs();
   createChildDialogStyleUpdate();
   $("#createChildDialog").dialog( "open" );
}

function createChildBaseTypeSelectChangeHandler() {
   createChildDialogStyleUpdate();
}

function createChildDialogStyleUpdate() {
   var baseType = $("#createChildBaseTypeSelect").val();
   var chv = canHaveValue(baseType);
   $("#createChildValueSpan").css("color",chv?"#000000":"#808080");
   $("#createChildValue").css("color",chv?"#000000":"#808080");
   $("#createChildValue").prop('disabled',!chv);
}

function createChildButtonHandler() {
    var node = g_selectedNode;
    var base = $("#createChildBaseTypeSelect").val();
    var name = $("#createChildName").val();
    var value = $("#createChildValue").val();
    var baseType = $("#createChildBaseTypeSelect").val();
    var chv = canHaveValue(baseType);

    var path = node.getPath();
    if (name.startsWith("$")) {
            $.ajax({
                url: path+"/"+name,
                type: 'PUT',
                contentType: "application/json",
                data: chv? "{\"$name\":\""+name+"\", \"$base\":\""+base+"\", \"$value\":\""+value+"\" }" : "{\"$name\":\""+name+"\", \"$base\":\""+base+"\" }",
                error: genericErrorHandler,
                success: function(result) {
                   setResult("OK");
                   node.refresh(); // these are async, so do refresh only after POST completes
                }
            });
    }
    else {
        $.ajax({
            url: path,
            type: 'POST',
            contentType: "application/json",
            data: "{\"$name\":\""+name+"\",\"$base\":\""+base+"\", \"$value\":\""+value+"\"}",
            error: genericErrorHandler,
            success: function(result) {
               setResult("OK");
               node.refresh(); // these are async, so do refresh only after POST completes
            }
        });
    }
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
/////////////////////////// NEW INSTANCE ///////////////////////////////
////////////////////////////////////////////////////////////////////////


var definitionNames;

function createInstanceDialogButtonHandler() {
   closeDialogs();
   // first check the definition list to see if anything changed
   $.get("/bws/.defs/$children")
      .fail(genericErrorHandler)
      .done(function(data, textStatus, jqXHR) {
          setResult("OK");
          if (data.$value !== definitionNames) {  // only do this if the list changes (so the user's selection does not get forgotten)
             definitionNames = data.$value;
             $("#createInstanceTypeSelect").find("option").remove();  // remove all existing <option>s  of the <select>
             //// add all builtins
             //var values = ["Null","Boolean","Unsigned","Integer","Real","Double","OctetString","String","BitString","Enumerated",
             //   "Date","DatePattern","DateTime","DateTimePattern","Time","TimePattern","ObjectIdentifier","ObjectIdentifierPattern",
             //   "WeekNDay","Sequence","Array","List","SequenceOf","Choice","Object","Bit","Link","Any","StringSet","Composition",
             //   "Collection","Unknown","Raw"];
             //for (i=0; i< values.length; i++) $("#createInstanceTypeSelect").append($("<option></option>").attr("value",values[i]).text(values[i]));
             // then add all /.defs names
             var values = data.$value.split(";");
             for (i=0; i< values.length; i++) $("#createInstanceTypeSelect").append($("<option></option>").attr("value",values[i]).text(values[i]));
         }
         $("#createInstanceDialog").dialog("open"); // OK, list is up to date, open dialog.
      });
}

function createInstanceMemberMinInputHandler(input) {
   var input = $(this);
   rangedNumericInputChangeHandlerHelper(input);
   var min = parseInt(input.val());
   var max = parseInt($("#createInstanceMemberMaxInput").val());
   if (min >= max) $("#createInstanceMemberMaxInput").val(min+1);
}

function createInstanceMemberMaxInputHandler(input) {
   var input = $(this);
   rangedNumericInputChangeHandlerHelper(input);
   var min = parseInt($("#createInstanceMemberMinInput").val());
   var max = parseInt(input.val());
   if (min >= max) $("#createInstanceMemberMinInput").val(max-1);
}

function rangedNumericInputChangeHandlerHelper(input) {
   var min = parseInt(input.attr("min"));
   var max = parseInt(input.attr("max"));
   var value = parseInt(input.val());
   if (value < min) input.val(min);
   if (value > max) input.val(max);
}

function rangedNumericInputChangeHandler() {
   rangedNumericInputChangeHandlerHelper($(this));
}

function createInstanceButtonHandler() {
    var node  = g_selectedNode;
    var type  = $("#createInstanceTypeSelect").val(); // $("#createInstanceTypeName").val();
    var name  = $("#createInstanceDataName").val();
    var opt   = $("#createInstanceOptionalAll").is(":checked")?"all":$("#createInstanceOptionalNone").is(":checked")?"none":"random";// optionals is "all", "none", or "random"
    var init  = $("#createInstanceInitDefault").is(":checked")?"default":"random";   // init values is "default" or "random"
    var memb  = $("#createInstanceMembersNone").is(":checked")?"none":"random";   // members is "none" or "random"
    var maxd  = $("#createInstanceDepthLimitInput").val();
    var maxc  = $("#createInstanceCountLimitInput").val();
    var optp  = $("#createInstanceOptionalPercentInput").val();
    var minm  = $("#createInstanceMemberMinInput").val();
    var maxm  = $("#createInstanceMemberMaxInput").val();
    var path  = node.getPath();
    $.post("/ui/rpc?"+$.param({ op:"createInstance", type:type, path:path, name:name, opt:opt, init:init, memb:memb, maxd:maxd, maxc:maxc, optp:optp, minm:minm, maxm:maxm }))
        .fail(genericErrorHandler)
        .done(function(data, textStatus, jqXHR) {
            setResult('OK');
            node.refresh();
        });
   closeDialogs();
}


////////////////////////////////////////////////////////////////////////
/////////////////////////// NEW SPECIAL ////////////////////////////////
////////////////////////////////////////////////////////////////////////

function createSpecialDialogButtonHandler() {
   closeDialogs();
   $("#createSpecialDialog").dialog("open");
}

function createCSMLButtonHandler() {
    var node  = g_selectedNode;
    var base  = "Collection";
    var name  = ".csml";
    var path  = node.getPath();
    $.ajax({
        url: path,
        type: 'POST',
        contentType: "application/json",
        data: "{\"$name\":\""+name+"\",\"$base\":\""+base+"\"}",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           node.refresh();
        }
    });
    closeDialogs();
}

function createDefinitionsButtonHandler() {
    var node  = g_selectedNode;
    var base  = "Collection";
    var name  = "$org.ampii.ui.definitions";
    var path  = node.getPath();
    $.ajax({
        url: path+"/"+name,
        type: 'PUT',
        contentType: "application/json",
        data: "{\"$base\":\""+base+"\"}",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           node.refresh();
        }
    });
    closeDialogs();
}
function createTagDefinitionsButtonHandler() {
    var node  = g_selectedNode;
    var base  = "Collection";
    var name  = "$org.ampii.ui.tagDefinitions";
    var path  = node.getPath();
    $.ajax({
        url: path+"/"+name,
        type: 'PUT',
        contentType: "application/json",
        data: "{\"$base\":\""+base+"\"}",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           node.refresh();
        }
    });
    closeDialogs();
}

////////////////////////////////////////////////////////////////////////
///////////////////////////// DELETE ///////////////////////////////////
////////////////////////////////////////////////////////////////////////


 function deleteButtonHandler() {
   var node = g_selectedNode;
   if (node.name === "$writable") {
      areYouSureDialog("Are you sure you want to delete '$writable'? That sounds dangerous and unrecoverable!",doDelete,closeDialogs);
   }
   else doDelete();
 }
 
 function doDelete() {
     var node = g_selectedNode;
     var path = node.getPath();
     $.ajax({
         url: path,
         type: 'DELETE',
         error: genericErrorHandler,
         success: function(result) {
            setResult("OK");
            node.parent.refresh(); // refresh parent after DELETE completes
         },
     });
    closeDialogs();
 }
 

////////////////////////////////////////////////////////////////////////
/////////////////////////// EDIT VALUE /////////////////////////////////
////////////////////////////////////////////////////////////////////////

function changeValueHistoryChangeHandler() {
   $("#changeValueValue").val($("#changeValueHistory").val());
}

function changeValueDialogButtonHandler() {
   closeDialogs();
   var node = g_selectedNode;
   $("#changeValueValue").val(node.value);
   $("#changeValueDialog").dialog( "open" );
}

function changeValueButtonHandler() {
   var node = g_selectedNode;
   var value = $("#changeValueValue").val();
   if (node.name === "$writable" && value !== 'true') {
      areYouSureDialog("Are you sure you want to set '$writable' to false? That sounds dangerous and unrecoverable!",doChange,closeDialogs);
   }
   else doChange();
}

function doChange() {
    var node = g_selectedNode;
    var value = $("#changeValueValue").val();
    var path = node.getPath();
    $.ajax({
        url: path,
        type: 'PUT',
        contentType: "application/json",
        data: "{\"$value\":\""+value+"\"}",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           node.refresh(); // these are async, so do refresh only after PUT completes
           if (node.name === "$displayName" || node.name === "$nodeType") node.parent.refreshInfo(); // I might have changed something in my parent's info display
           // add to history (if not already there)
           if (!($("#changeValueHistory option[value='"+value+"']").length > 0)) $("#changeValueHistory").append($("<option></option>").attr("value",value).text(value))
        }
    });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
//////////////////////////////  CUT  ///////////////////////////////////
////////////////////////////////////////////////////////////////////////

function cutButtonHandler() {
  var node = g_selectedNode;
  if (node.name === "$writable") {
     areYouSureDialog("Are you sure you want to delete '$writable'? That sounds dangerous and unrecoverable!",doCut,closeDialogs);
  }
  else doCut();
}

function doCut() {
    // do a copy then a delete
    var node = g_selectedNode;
    var path = node.getPath();
    $.ajax({
        url: path+"?metadata=all,-self&alt="+($('#clipboardFormatXML').is(":checked")?"xml":"json"),
        type: 'GET',
        dataType: "text",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           text = result;
           $("#clipboard").val(text);
           clipboardAddToHistory(path,text);
           lines = text.split("\n").length;
           $("#clipboard").attr("rows",lines);
           // this is asynchronous, so we have to to the DELETE after the GET completes
           $.ajax({
               url: path,
               type: 'DELETE',
               error: genericErrorHandler,
               success: function(result) {
                  setResult("OK");
                  node.parent.refresh(); // refresh parent only after DELETE completes
               },
           });
        },
    });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
//////////////////////////////  COPY  //////////////////////////////////
////////////////////////////////////////////////////////////////////////

function copyButtonHandler() {
    var node = g_selectedNode;
    var path = node.getPath();
    var url = path+"?metadata=all,-self&alt="+($('#clipboardFormatXML').is(":checked")?"xml":"json");
    $.ajax({
        url: url,
        type: 'GET',
        dataType: "text",
        error: genericErrorHandler,
        success: function(result) {
           setResult("OK");
           text = result;
           $("#clipboard").val(text);
           clipboardAddToHistory(path,text);
           lines = text.split("\n").length;    // resize after change text
           $("#clipboard").attr("rows",lines);
        },
    });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
////////////////////////////  PASTE  ///////////////////////////////////
////////////////////////////////////////////////////////////////////////

function pasteOntoButtonHandler() {
   var node = g_selectedNode;
   var data = $("#clipboard").val();
   var alt = data.startsWith("{")? "json" : "xml";
   $.ajax({
       url: node.getPath()+"?alt="+alt,
       type: 'PUT',
       contentType: alt === 'xml'? "application/xml" : "application/json",
       data: data,
       error: genericErrorHandler,
       success: function(result) {
          setResult("OK");
          node.refresh();
       },
   });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
///////////////////////////  PASTE UNDER  //////////////////////////////
////////////////////////////////////////////////////////////////////////

function pasteUnderButtonHandler() {
   var node = g_selectedNode;
   var data = $("#clipboard").val();
   var alt = data.startsWith("{")? "json" : "xml";
   $.ajax({
       url: node.getPath()+"?alt="+alt,
       type: 'POST',
       contentType: alt === 'xml'? "application/xml" : "application/json",
       data: data,
       error: genericErrorHandler,
       success: function(result) {
          setResult("OK");
          node.refresh();
       },
   });
   closeDialogs();
}

////////////////////////////////////////////////////////////////////////
/////////////////////////  CLIPBOARD TAB  //////////////////////////////
////////////////////////////////////////////////////////////////////////

function clipboardActionPaneButtonClick() {
    setResult("");
    showActionSection('#clipboardActionPane');
}

var clipboardHistoryIndex = 0;  // increments on first insertion to 1 and wraps back to 1
var clipboardHistoryTexts = [];
var clipboardHistoryPaths = [];
var clipboardHistorySize  = 10;

function clipboardDuplicateButtonHandler() {
   var currentIndex = $("#clipboardHistorySelect").val();
   var text = clipboardHistoryTexts[currentIndex];
   var path = clipboardHistoryPaths[currentIndex];
   clipboardAddToHistory(path,text);
   clipboardResize();
}

function clipboardAddToHistory(path,text) {
   if (++clipboardHistoryIndex > clipboardHistorySize) clipboardHistoryIndex = 1;  // get new index (wraps to 1)
   clipboardHistoryTexts[clipboardHistoryIndex] = text;
   clipboardHistoryPaths[clipboardHistoryIndex] = path;
   var existing = $("#clipboardHistorySelect option[value="+clipboardHistoryIndex+"]");
   if (existing.length)   // if that option already exists
     existing.text(clipboardHistoryIndex+":"+path); // then just change the display
   else  // else append a new option
      $("#clipboardHistorySelect").append($("<option></option>").attr("value",clipboardHistoryIndex).text(clipboardHistoryIndex+":"+path));
   $("#clipboardHistorySelect").val(clipboardHistoryIndex); // set the select to the current indes so it matches the text in the textarea
}

function clipboardHistorySelectChangeHandler() {
   var text = clipboardHistoryTexts[$("#clipboardHistorySelect").val()];
   $("#clipboard").val(text);
   clipboardResize();
}

function clipboardOnKeyUp() {  // autosize the clipboard textarea and remember the edits in the history entry
   clipboardHistoryTexts[$("#clipboardHistorySelect").val()] = $("#clipboard").val();
   clipboardResize();
}

function clipboardResize() {
   var lines = $("#clipboard").val().split("\n").length;
   $("#clipboard").attr("rows",lines);
}

////////////////////////////////////////////////////////////////////////
//////////////////////  ARE YOU SURE DIALOG  ///////////////////////////
////////////////////////////////////////////////////////////////////////

function areYouSureDialog(prompt,yesAction,noAction) {
   $("#areYouSurePrompt").html(prompt);
   $("#areYouSureDialog").dialog({
      modal: true,
      buttons: { 'No': noAction, 'Yes': yesAction },
   });
   $("#areYouSureDialog").dialog("open");
}


////////////////////////////////////////////////////////////////////////
//////////////////////  PATH EXPLORER TAB  /////////////////////////////
////////////////////////////////////////////////////////////////////////

/*function pathExplorerActionPaneButtonClick() {
    setResult("");
    showActionSection('#pathExplorerActionPane');
}

function pathGoButtonClick() {
    if ($('#method').val() === 'GET') {
            $.ajax({
                url: $('#url').val() + '?alt=' + $('#alt').val() + ($('#metadata').val()==='none'? '':'&metadata='+$('#metadata').val()),
                type: "GET",
                dataType: $('#alt').val(),
                error: function(xhr, textStatus, errorThrown) {
                    $('#result').html('<p>HTTP Status: ' + xhr.status + ' ' + errorThrown + '</p><p>Response body:</p><div><pre>'+escapeHtml(xhr.responseText) + '</pre></div>');
                },
                success: function(data, textStatus, jqXHR) {
                    $('#result').html('');
                    if ($('#alt').val() === 'plain') {
                       $("#result").append(data);
                    }
                    else if ($('#alt').val() === 'xml') {
                        $("#result").append("attributes = { ");
                        $.each(data.documentElement.attributes, function(i, attr) {
                              $("#result").append(attr.name + " = <i>" + attr.nodeValue + "</i><br/>");
                        });
                        $("#result").append("} ");
                        $("#result").append("children = { ")
                        $.each(data.documentElement.children, function(i, child) {
                              $("#result").append(child.nodeName + "( " );
                              $.each(child.attributes, function(i, attr) {
                                     $("#result").append(attr.name + " = <i>" + attr.nodeValue + "</i><br/>");
                              });
                              $("#result").append(") ");
                        });
                        $("#result").append("} ")
                    }
                    else {
                        $.each(data, function(i, field){
                             $("#result").append(i + " = <i>" + field + "</i><br/>");
                        });
                    }
                }
            });
    }
    else if ($('#method').val() === 'POST') {
        $.ajax({
            url: $('#url').val() + '?alt=' + $('#alt').val(),
            type: "POST",
            data: $('#data').val(),
            error: function(xhr, textStatus, errorThrown) {
                $('#result').html('<p>HTTP Status: ' + xhr.status + ' ' + errorThrown + '</p><p>Response body:</p><div><pre>'+escapeHtml(xhr.responseText) + '</pre></div>');
            },
            success: function(data, textStatus, jqXHR) {
                $('#result').html('<p>HTTP Status: ' + jqXHR.status + '</p><p>Response body:</p><div><pre>'+escapeHtml(jqXHR.responseText) + '</pre></div>');
            }
        });
    }
    else if ($('#method').val() === 'PUT') {
        $.ajax({
            url: $('#url').val() + '?alt=' + $('#alt').val(),
            type: "PUT",
            data: $('#data').val(),
            error: function(xhr, textStatus, errorThrown) {
                $('#result').html('<p>HTTP Status: ' + xhr.status + ' ' + errorThrown + '</p><p>Response body:</p><div><pre>'+escapeHtml(xhr.responseText) + '</pre></div>');
            },
            success: function(data, textStatus, jqXHR) {
                $('#result').html('<p>HTTP Status: ' + jqXHR.status + '</p><p>Response body:</p><div><pre>'+escapeHtml(jqXHR.responseText) + '</pre></div>');
            }
        });
    }
    else if ($('#method').val() === 'DELETE') {
        $.ajax({
            url: $('#url').val() + '?alt=' + $('#alt').val(),
            type: "DELETE",
            error: function(xhr, textStatus, errorThrown) {
                $('#result').html('<p>HTTP Status: ' + xhr.status + ' ' + errorThrown + '</p><p>Response body:</p><div><pre>'+escapeHtml(xhr.responseText) + '</pre></div>');
            },
            success: function(data, textStatus, jqXHR) {
                $('#result').html('<p>HTTP Status: ' + jqXHR.status + '</p><p>Response body:</p><div><pre>'+escapeHtml(jqXHR.responseText) + '</pre></div>');
            }
        });
    }
}

function methodChange() {
   if ($('#method').val() === 'GET' || $('#method').val() === 'DELETE') $('#dataField').hide();
   else $('#dataField').show();
}

function dataOnKeyUp() {  // autosize the data textarea
   $(this).height( 0 );
   $(this).height( this.scrollHeight );
}
*/


////////// misc utilities /////////////////

function hexEncode(s){
    var hex, i;
    var result = "";
    for (i=0; i<s.length; i++) {
        hex = s.charCodeAt(i).toString(16);
        result += ("000"+hex).slice(-4);
    }
    return result
}

var entityMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': '&quot;',
    "'": '&#39;',
    "/": '&#x2F;'
}

function escapeHtml(string) {
  return String(string).replace(/[&<>"'\/]/g, function (s) {
    return entityMap[s];
  });
}

function hexEncode(s){
    var hex, i;
    var result = "";
    for (i=0; i<s.length; i++) {
        hex = s.charCodeAt(i).toString(16);
        result += ("000"+hex).slice(-4);
    }
    return result
}
