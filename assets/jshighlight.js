var MyApp_SearchResultCount = 0;

function MyApp_Test() {
  document.bgColor="#AAAAAA";
}

function MyApp_HighlightAllOccurencesOfString(keyword) {
  MyApp_RemoveAllHighlights();
  MyApp_HighlightAllOccurencesOfStringForElement(document.body, keyword.toLowerCase());
  document.getElementById("MyAppHighlight").scrollIntoView();
  jsInterface.receiveValueFromJS(MyApp_SearchResultCount);
}

function MyApp_HighlightAllOccurencesOfStringForElement(element,keyword) {
  if (element) {
    if (element.nodeType == 3) {        
      while (true) {
        var value = element.nodeValue;  
        var idx = value.toLowerCase().indexOf(keyword);

        if (idx < 0) break;           

        var span = document.createElement("span");
        var text = document.createTextNode(value.substr(idx,keyword.length));
        span.appendChild(text);
        span.setAttribute("class","MyAppHighlight");
		span.setAttribute("id","MyAppHighlight");
        span.style.backgroundColor="yellow";
        span.style.color="black";
        text = document.createTextNode(value.substr(idx+keyword.length));
        element.deleteData(idx, value.length - idx);
        var next = element.nextSibling;
        element.parentNode.insertBefore(span, next);
        element.parentNode.insertBefore(text, next);
        element = text;
        MyApp_SearchResultCount++;
      }
    } else if (element.nodeType == 1) { 
      if (element.style.display != "none" && element.nodeName.toLowerCase() != 'select') {
        for (var i=element.childNodes.length-1; i>=0; i--) {
          MyApp_HighlightAllOccurencesOfStringForElement(element.childNodes[i],keyword);
        }
      }
    }
  }
}

function MyApp_RemoveAllHighlightsForElement(element) {
  if (element) {
    if (element.nodeType == 1) {
      if (element.getAttribute("class") == "MyAppHighlight") {
        var text = element.removeChild(element.firstChild);
        element.parentNode.insertBefore(text,element);
        element.parentNode.removeChild(element);
        return true;
      } else {
        var normalize = false;
        for (var i=element.childNodes.length-1; i>=0; i--) {
          if (MyApp_RemoveAllHighlightsForElement(element.childNodes[i])) {
            normalize = true;
          }
        }
        if (normalize) {
          element.normalize();
        }
      }
    }
  }
  return false;
}

function MyApp_RemoveAllHighlights() {
  MyApp_SearchResultCount = 0;
  MyApp_RemoveAllHighlightsForElement(document.body);
}
