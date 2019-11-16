(function () {

	WM.createHtmlEditView = function (elem) {

		function getSelectText() {
			var text;
			var select;
			if (window.getSelection)
				text = window.getSelection();
			else
				text = document.selection.createRange().text;
			return text;
		}
		function insertSelectTag(tag) {
			try {
				if (window.getSelection) {
					var select = window.getSelection();
					document.execCommand('inserthtml', false, tag + doc.getSelection());
				}
				else {
					var range = document.selection.createRange();
					range.pasteHTML(tag);
				}
			}
			catch (e) { }
		}
		function setSelectTag(start, end) {
		    if (!end)
		        end = "";
			try {
				if (window.getSelection) {
					var select = window.getSelection();
					var dumy = document.createElement('DIV');
					var r = select.getRangeAt(0);
					dumy.appendChild(r.cloneContents());
					r.deleteContents();
					r.insertNode(r.createContextualFragment(start + dumy.innerHTML + end));
					select.addRange(r);
				}
				else {
					var range = document.selection.createRange();
					range.pasteHTML(start + range.htmlText + end);
					window.getSelection().addRange(range);
				}
			}
			catch (e) { }
		}
		function replaseSelectTag(value) {
		    try {
		        if (window.getSelection) {
		            var select = window.getSelection();
		            var r = select.getRangeAt(0);
		            r.deleteContents();
		            r.insertNode(r.createContextualFragment(value));
		            select.addRange(r);
		        }
		        else {
		            var range = document.selection.createRange();
		            range.pasteHTML(value);
		            window.getSelection().addRange(range);
		        }
		    }
		    catch (e) { }
		}
		function delSelectTag() {
			try {
				if (window.getSelection) {
					var select = window.getSelection();
					var dumy = document.createElement('DIV');
					var r = select.getRangeAt(0);
					dumy.appendChild(r.cloneContents());
					r.deleteContents();
					r.insertNode(r.createContextualFragment(dumy.textContent));
					select.addRange(r);
				}
				else {
					var range = document.selection.createRange();
					range.pasteHTML(range.txt);
					window.getSelection().addRange(range);
				}
			}
			catch (e) { }
		}
		function getSelectRange() {
			var range;

			try {
				if (window.getSelection != null) {
					range = window.getSelection().getRangeAt(0);
				}
				else {
					range = document.selection.createRange();
				}
			}
			catch (e) { }
			if (range == null)
				return null;

			range.setTag = function (start, end) {
				try {
					if (window.getSelection) {
						var dumy = document.createElement('SPAN');
						var dumy2 = document.createElement('SPAN');
						var df = this.cloneContents();
						dumy.appendChild(df);
						dumy2.innerHTML = start + dumy.innerHTML + end;
						range.surroundContents(dumy2.childNodes[0]);
					}
					else {
						range.pasteHTML(start + this.htmlText + end);
					}
				}
				catch (e) { }
			}
			range.insertTag = function (tag) {
				//try
				{
					if (window.getSelection) {
						var dumy = document.createElement('SPAN');
						dumy.innerHTML = tag + dumy.innerHTML;
						range.insertNode(dumy.childNodes[0]);
					}
					else {
						range.pasteHTML(tag + this.htmlText);
					}
				}
				//catch(e){}
			}
			return range;
		}
		function createMovie() {
		    var frame = WM.createFrameWindow();
		    frame.setTitle('動画');
		    var button = WM.createButton();
		    button.setClientStyle(WM.STYLE_RIGHT);
		    button.setText('SET');
		    button.setWidth(32);
		    frame.addChild(button);

		    var range = getSelectRange();
		    if (range == null)
		        return false;
		    button.onclick = function () {
		        var link = text.getText();
		        var re = /watch\?v=(.*)/;
		        var id = link.match(re);
		        if (id)
		        {
		            var tag = AFL.sprintf(
                        "<div class=\"video-container\">"+
                        "<iframe src=\"http://www.youtube.com/embed/%s?hd=1&rel=0\" width=\"600\" \"400\" frameborder=\"0\" allowfullscreen=\"\"></iframe>"+
                        "</div>",id[1]);
		            if (range)
		                range.insertTag(tag);
		            else
		                insertSelectTag(tag);
		        }

		        frame.close();
		    }

		    var text = WM.createTextBox();
		    text.setClientStyle(WM.STYLE_CLIENT);
		    text.setText('http://');
		    frame.addChild(text);

		    frame.setPos();
		    frame.setTopMost(true);
		    frame.setSize(320, 48);
		}

		function createLink() {
			var frame = WM.createFrameWindow();
			frame.setTitle('リンク');
			var button = WM.createButton();
			button.setClientStyle(WM.STYLE_RIGHT);
			button.setText('SET');
			button.setWidth(32);
			frame.addChild(button);

			var range = getSelectRange();
			if (range == null)
				return false;
			button.onclick = function () {
				if (range)
					range.setTag(AFL.sprintf("<A href='%s'>", text.getText()), "</A>");
				else
					setSelectTag(AFL.sprintf("<A href='%s'>", text.getText()), "</A>");
				frame.close();
			}

			var text = WM.createTextBox();
			text.setClientStyle(WM.STYLE_CLIENT);
			text.setText('http://');
			frame.addChild(text);

			frame.setPos();
			frame.setTopMost(true);
			frame.setSize(320, 48);
		}
		function createTool(name, width) {
			var button = WM.createButton();
			button.setPriority(tools.getChildCount());
			button.style.fontSize = '10px';
			button.setClientStyle(WM.STYLE_LEFT);
			button.setWidth(width == null ? 24 : width);
			button.style.cursor = 'pointer';
			button.setText(name);
			tools.addChild(button);
			return button;

		}

		var tab = WM.createTab(elem);

		var htmlEdit = WM.createWindow();
		htmlEdit.setClientStyle(WM.STYLE_CLIENT);

		var textEdit = WM.createWindow();
		textEdit.setClientStyle(WM.STYLE_CLIENT);

		var tools = WM.createPanel();
		tools.setSize(24, 24);
		tools.setClientStyle(WM.STYLE_TOP);
		htmlEdit.addChild(tools);

		var mTextColor = 0xff000000;
		var mBackColor = 0xfffffff;
		function drawColorPicker(button,flag) {
			var colorPicker = WM.createColorPicker({ width: 200, height: 200 });
			document.body.appendChild(colorPicker);
			var frame = WM.createFrameWindow();
			frame.setHtml(colorPicker);
			frame.setSize(220, 230);
			frame.setTopMost(true);
			colorPicker.onColor = function (r, g, b)
			{
				var color = (255 << 24) + (r << 16) + (g << 8) + b;
				button.setTextColor(color);
				if (flag)
					mTextColor = color;
				else
					mBackColor = color;
			};
		}

		var toolsParam = [
			['解', , function () { delSelectTag(); return false; }],
			['左', , function () { setSelectTag("<DIV align='left'>", "</DIV>"); return false; }],
			['中', , function () { setSelectTag("<DIV align='center'>", "</DIV>"); return false; }],
			['右', , function () { setSelectTag("<DIV align='right'>", "</DIV>"); return false; }],
			['太', , function () { setSelectTag('<B>', '</B>'); return false; }],
			['斜', , function () { setSelectTag('<I>', '</I>'); return false; }],
			['文', , function () { setSelectTag('<SPAN style="color:' + WM.getARGB(mTextColor) + '">', '</SPAN>'); return false; }],
			['■', , function () { drawColorPicker(this,true);return false;}],
			['背', , function () { setSelectTag('<SPAN style="background-color:' + WM.getARGB(mBackColor) + '">', '</SPAN>'); return false; }],
			['■', , function () { drawColorPicker(this,false); return false; }],
			['F1', , function () { setSelectTag('<FONT size="1">', '</FONT>'); return false; }],
			['F2', , function () { setSelectTag('<FONT size="2">', '</FONT>'); return false; }],
			['F3', , function () { setSelectTag('<FONT size="3">', '</FONT>'); return false; }],
			['F4', , function () { setSelectTag('<FONT size="4">', '</FONT>'); return false; }],
			['F5', , function () { setSelectTag('<FONT size="5">', '</FONT>'); return false; }],
			['F6', , function () { setSelectTag('<FONT size="6">', '</FONT>'); return false; }],
			['F7', , function () { setSelectTag('<FONT size="7">', '</FONT>'); return false; }],
		];
		for (var index in toolsParam)
		{
			var param = toolsParam[index];
			var editTool = createTool(param[0], param[1]);
			editTool.onmousedown = param[2];
		}

		var editTool = createTool('A');
		editTool.onmousedown = function () { createLink(); return false; }

		var editTool = createTool('動');
		editTool.onmousedown = function () { createMovie(); return false; }

		var editTool = createTool('CODE', 48);
		editTool.onclick = function () {
		    setSelectTag('<DIV class="code">', '</DIV>');
		    return false;
		}


		var editTool = createTool('FILE', 48);
		editTool.onclick = function () {
			if (!tab.getFile)
				return false;
			var fileList = tab.getFile();
			fileList.setPos();

			fileList.onExec = function (id,name,url) {
				textArea.focus();
				var tag = AFL.sprintf("<A data-id='%d' HREF='%s'>%s</A>",id, url, name);
				var range = getSelectRange();
				if (range)
					range.insertTag(tag);
				else
					insertSelectTag(tag);
			}
			return false;
		}
		var editTool = createTool('IMG', 42);
		editTool.onclick = function () {
			var fileList = tab.getFile();
			fileList.setPos();
			fileList.onExec = function (id, name,url) {
				textArea.focus();
				var tag = AFL.sprintf("<IMAGE  data-id='%d' src='%s' ALT='%s' border='0'/>",id, url, name);
				var range = getSelectRange();
				if (range)
					range.insertTag(tag);
				else
					insertSelectTag(tag);
			}
			return false;
		}

		var edit = WM.createWindow();
		edit.setClientStyle(WM.STYLE_CLIENT);
		edit.setBackgroundColor(0xccffffff);
		htmlEdit.addChild(edit);

		var textArea = document.createElement("DIV");
		textArea.innerHTML="<br>"
		textArea.contentEditable = 'true';
		textArea.style.margin = '1px';
		textArea.style.overflow = 'auto';
		edit.appendChild(textArea);

		textArea.onkeypress = function (e) {
			var c = e.keyCode;
			if (c == null)
				c = e["char"];

			if (c == 13) {
				var r = window.getSelection().getRangeAt(0);
				var node = document.createElement("br");
				r.insertNode(node);
				var r = document.createRange();
				r.setStartAfter(node, 0);
				r.setEndAfter(node, 0);
				window.getSelection().removeAllRanges();
				window.getSelection().addRange(r);
				e.preventDefault();
			}
		}
		textArea.onpaste = function (e) {
		    var pastedData = e.clipboardData?e.clipboardData.getData("text/plain"):window.clipboardData.getData('Text');
		    var node = document.createElement("pre");
		    node.textContent = pastedData;
		    var text = node.innerHTML.replace(/(?:\r\n|\r|\n)/g, '<br/>');
		    text = text.replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;');
		    text = text.replace(/ /g, '&nbsp;');
		    var range = getSelectRange();
		    //if (range)
		    //    range.setHtml(text);
		    //else
		    replaseSelectTag(text);



		    e.preventDefault();
		    return false;
		}
		textArea.onfocus = function()
		{
			var range = document.createRange();
			range.setStart(textArea, 0);
			range.setEnd(textArea, 0);
			window.getSelection().removeAllRanges();
			window.getSelection().addRange(range);
		}


		var textAreaText = document.createElement("TEXTAREA");
		textAreaText.style.position = 'absolute';
		//textAreaText.style.overflow = 'auto';
		textEdit.appendChild(textAreaText);
		textAreaText.innerHTML = "<br>";

		var textMove = function () {
			textAreaText.style.width = this.getClientWidth() + 'px';
			textAreaText.style.height = this.getClientHeight() + 'px';
			return true;
		}
		var htmlMove = function () {
			textArea.style.width = this.getClientWidth() + 'px';
			textArea.style.height = this.getClientHeight() + 'px';
			return true;
		}
		textEdit.addEvent("onSized", textMove);
		edit.addEvent("onSized", htmlMove);
		tab.setName = function (name) {
			textArea.name = name;
		}
		tab.getHtmlNode = function()
		{
			return textArea.cloneNode(true);
		}
		tab.setValue = function (value) {
			textArea.innerHTML = value;
			textAreaText.value = value;
		}
		tab.getValue = function () {
			if (this.getSelectItem() == 0)
				return textArea.innerHTML;
			else
				return textAreaText.value;
		}
		tab.onChange = function (index) {
			if (this.getSelectItem() == 0) {
				textArea.innerHTML = textAreaText.value;
			}
			else
				textAreaText.value = textArea.innerHTML;
		}

		tab.addItem('HTML', htmlEdit);
		tab.addItem('TEXT', textEdit);


		return tab;
	}



})();