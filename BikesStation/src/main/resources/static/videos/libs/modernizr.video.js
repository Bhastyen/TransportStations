window.Modernizr=function(p,d,t){function k(b,a){for(var c in b)if(D[b[c]]!==t)return"pfx"==a?b[c]:!0;return!1}var f={},B=d.documentElement;d.head||d.getElementsByTagName("head");var m=d.createElement("modernizr"),D=m.style,E=["Webkit","Moz","O","ms","Khtml"],m={},C=[],z=function(){var b={select:"input",change:"input",submit:"form",reset:"form",error:"img",load:"img",abort:"img"};return function(a,c){c=c||d.createElement(b[a]||"div");a="on"+a;var w=a in c;w||(c.setAttribute||(c=d.createElement("div")),
c.setAttribute&&c.removeAttribute&&(c.setAttribute(a,""),w="function"===typeof c[a],typeof c[a]===t||(c[a]=t),c.removeAttribute(a)));return w}}(),e,q={}.hasOwnProperty,x;typeof q!==t&&typeof q.call!==t?x=function(b,a){return q.call(b,a)}:x=function(b,a){return a in b&&typeof b.constructor.prototype[a]===t};m.video=function(){var b=d.createElement("video"),a=!1;try{if(a=!!b.canPlayType)a=new Boolean(a),a.ogg=b.canPlayType('video/ogg; codecs="theora"'),a.h264=b.canPlayType('video/mp4; codecs="avc1.42E01E"')||
b.canPlayType('video/mp4; codecs="avc1.42E01E, mp4a.40.2"'),a.webm=b.canPlayType('video/webm; codecs="vp8, vorbis"')}catch(c){}return a};m.audio=function(){var b=d.createElement("audio"),a=!1;try{if(a=!!b.canPlayType)a=new Boolean(a),a.ogg=b.canPlayType('audio/ogg; codecs="vorbis"'),a.mp3=b.canPlayType("audio/mpeg;"),a.wav=b.canPlayType('audio/wav; codecs="1"'),a.m4a=b.canPlayType("audio/x-m4a;")||b.canPlayType("audio/aac;")}catch(c){}return a};for(var n in m)x(m,n)&&(e=n.toLowerCase(),f[e]=m[n](),
C.push((f[e]?"":"no-")+e));D.cssText="";m=null;p.attachEvent&&function(){var b=d.createElement("div");b.innerHTML="<elem></elem>";return 1!==b.childNodes.length}()&&function(b,a){function c(h){for(var a=-1;++a<k;)h.createElement(g[a])}b.iepp=b.iepp||{};var d=b.iepp,e=d.html5elements||"abbr|article|aside|audio|canvas|datalist|details|figcaption|figure|footer|header|hgroup|mark|meter|nav|output|progress|section|summary|time|video",g=e.split("|"),k=g.length,f=RegExp("(^|\\s)("+e+")","gi"),m=RegExp("<(/*)("+
e+")","gi"),n=/^\s*[\{\}]\s*$/,q=RegExp("(^|[^\\n]*?\\s)("+e+")([^\\n]*)({[\\n\\w\\W]*?})","gi"),p=a.createDocumentFragment(),u=a.documentElement,e=u.firstChild,r=a.createElement("body"),v=a.createElement("style"),s=/print|all/,h;d.getCSS=function(h,a){if(h+""===t)return"";for(var b=-1,c=h.length,e,g=[];++b<c;)e=h[b],e.disabled||(a=e.media||a,s.test(a)&&g.push(d.getCSS(e.imports,a),e.cssText),a="all");return g.join("")};d.parseCSS=function(h){for(var a=[],b;null!=(b=q.exec(h));)a.push(((n.exec(b[1])?
"\n":b[1])+b[2]+b[3]).replace(f,"$1.iepp_$2")+b[4]);return a.join("\n")};d.writeHTML=function(){var b=-1;for(h=h||a.body;++b<k;)for(var d=a.getElementsByTagName(g[b]),c=d.length,e=-1;++e<c;)0>d[e].className.indexOf("iepp_")&&(d[e].className+=" iepp_"+g[b]);p.appendChild(h);u.appendChild(r);r.className=h.className;r.id=h.id;r.innerHTML=h.innerHTML.replace(m,"<$1font")};d._beforePrint=function(){v.styleSheet.cssText=d.parseCSS(d.getCSS(a.styleSheets,"all"));d.writeHTML()};d.restoreHTML=function(){r.innerHTML=
"";u.removeChild(r);u.appendChild(h)};d._afterPrint=function(){d.restoreHTML();v.styleSheet.cssText=""};c(a);c(p);d.disablePP||(e.insertBefore(v,e.firstChild),v.media="print",v.className="iepp-printshim",b.attachEvent("onbeforeprint",d._beforePrint),b.attachEvent("onafterprint",d._afterPrint))}(p,d);f._version="2.0.6";f._prefixes=" -webkit- -moz- -o- -ms- -khtml- ".split(" ");f._domPrefixes=E;f.hasEvent=z;f.testProp=function(b){return k([b])};f.testAllProps=function(b,a){var d=b.charAt(0).toUpperCase()+
b.substr(1),d=(b+" "+E.join(d+" ")+d).split(" ");return k(d,a)};f.testStyles=function(b,a,c,e){var k,g=d.createElement("div");if(parseInt(c,10))for(;c--;)k=d.createElement("div"),k.id=e?e[c]:"modernizr"+(c+1),g.appendChild(k);c=["&shy;<style>",b,"</style>"].join("");g.id="modernizr";g.innerHTML+=c;B.appendChild(g);b=a(g,b);g.parentNode.removeChild(g);return!!b};B.className=B.className.replace(/\bno-js\b/,"")+(" js "+C.join(" "));return f}(this,this.document);
(function(p,d,t){function k(){for(var h=1,b=-1;n.length-++b&&(!n[b].s||(h=n[b].r)););h&&m()}function f(b){var a=d.createElement("script"),c;a.src=b.s;a.onreadystatechange=a.onload=function(){c||a.readyState&&"loaded"!=a.readyState&&"complete"!=a.readyState||(c=1,k(),a.onload=a.onreadystatechange=null)};e(function(){c||(c=1,k())},s.errorTimeout);b.e?a.onload():q.parentNode.insertBefore(a,q)}function B(b){var c=d.createElement("link"),y;c.href=b.s;c.rel="stylesheet";c.type="text/css";if(b.e||!g&&!a)c.onload=
function(){y||(y=1,e(function(){k()},0))},b.e&&c.onload();else{var f=function(b){e(function(){if(!y)try{b.sheet.cssRules.length?(y=1,k()):f(b)}catch(a){1E3==a.code||"security"==a.message||"denied"==a.message?(y=1,e(function(){k()},0)):f(b)}},0)};f(c)}e(function(){y||(y=1,k())},s.errorTimeout);!b.e&&q.parentNode.insertBefore(c,q)}function m(){var a=n.shift();b=1;a?a.t?e(function(){"c"==a.t?B(a):f(a)},0):(a(),k()):b=0}function D(a,g,f,p,t,N){function r(){G||l.readyState&&"loaded"!=l.readyState&&"complete"!=
l.readyState||(A.r=G=1,!b&&k(),l.onload=l.onreadystatechange=null,e(function(){w.removeChild(l)},0))}var l=d.createElement(a),G=0,A={t:f,s:g,e:N};l.src=l.data=g;!c&&(l.style.display="none");l.width=l.height="0";"object"!=a&&(l.type=f);l.onload=l.onreadystatechange=r;"img"==a?l.onerror=r:"script"==a&&(l.onerror=function(){A.e=A.r=1;m()});n.splice(p,0,A);w.insertBefore(l,c?null:q);e(function(){G||(w.removeChild(l),A.r=A.e=G=1,k())},s.errorTimeout)}function E(a,d,c){var e="c"==d?M:I;b=0;d=d||"j";F(a)?
D(e,a,d,this.i++,z,c):(n.splice(this.i++,0,a),1==n.length&&m());return this}function C(){var a=s;a.loader={load:E,i:0};return a}var z=d.documentElement,e=p.setTimeout,q=d.getElementsByTagName("script")[0],x={}.toString,n=[],b=0,a="MozAppearance"in z.style,c=a&&!!d.createRange().compareNode,w=c?z:q.parentNode,L=p.opera&&"[object Opera]"==x.call(p.opera),g="webkitAppearance"in z.style,K=g&&"async"in d.createElement("script"),I=a?"object":L||K?"img":"script",M=g?"img":I,J=Array.isArray||function(a){return"[object Array]"==
x.call(a)},F=function(a){return"string"==typeof a},H=function(a){return"[object Function]"==x.call(a)},u=[],r={},v,s;s=function(a){function b(a){a=a.split("!");var d=u.length,c=a.pop(),e=a.length,c={url:c,origUrl:c,prefixes:a},g,f;for(f=0;f<e;f++)(g=r[a[f]])&&(c=g(c));for(f=0;f<d;f++)c=u[f](c);return c}function d(a,c,e,f,g){var h=b(a),k=h.autoCallback;if(!h.bypass){c&&(c=H(c)?c:c[a]||c[f]||c[a.split("/").pop().split("?")[0]]);if(h.instead)return h.instead(a,c,e,f,g);e.load(h.url,h.forceCSS||!h.forceJS&&
/css$/.test(h.url)?"c":t,h.noexec);(H(c)||H(k))&&e.load(function(){C();c&&c(h.origUrl,g,f);k&&k(h.origUrl,g,f)})}}function c(a,b){function e(a){if(F(a))d(a,h,b,0,f);else if(Object(a)===a)for(k in a)a.hasOwnProperty(k)&&d(a[k],h,b,k,f)}var f=!!a.test,g=a.load||a.both,h=a.callback,k;e(f?a.yep:a.nope);e(g);a.complete&&b.load(a.complete)}var e,g,f=this.yepnope.loader;if(F(a))d(a,0,f,0);else if(J(a))for(e=0;e<a.length;e++)g=a[e],F(g)?d(g,0,f,0):J(g)?s(g):Object(g)===g&&c(g,f);else Object(a)===a&&c(a,f)};
s.addPrefix=function(a,b){r[a]=b};s.addFilter=function(a){u.push(a)};s.errorTimeout=1E4;null==d.readyState&&d.addEventListener&&(d.readyState="loading",d.addEventListener("DOMContentLoaded",v=function(){d.removeEventListener("DOMContentLoaded",v,0);d.readyState="complete"},0));p.yepnope=C()})(this,this.document);Modernizr.load=function(){yepnope.apply(window,[].slice.call(arguments,0))};
