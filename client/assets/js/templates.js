(function() {
  var template = Handlebars.template, templates = Handlebars.templates = Handlebars.templates || {};
templates['thumbnail'] = template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, functionType="function", escapeExpression=this.escapeExpression;


  buffer += "<p>";
  if (stack1 = helpers.name) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = (depth0 && depth0.name); stack1 = typeof stack1 === functionType ? stack1.call(depth0, {hash:{},data:data}) : stack1; }
  buffer += escapeExpression(stack1)
    + "</p>\n<img class=\"mini-icon\" src=\"";
  if (stack1 = helpers.thumbnail) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = (depth0 && depth0.thumbnail); stack1 = typeof stack1 === functionType ? stack1.call(depth0, {hash:{},data:data}) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n<p class=\"repo-description\">";
  if (stack1 = helpers.description) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = (depth0 && depth0.description); stack1 = typeof stack1 === functionType ? stack1.call(depth0, {hash:{},data:data}) : stack1; }
  buffer += escapeExpression(stack1)
    + "</p>";
  return buffer;
  });
templates['video'] = template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, functionType="function", escapeExpression=this.escapeExpression, helperMissing=helpers.helperMissing, self=this;

function program1(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n	<tr class=\"video\" data-video-id=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = (depth0 && depth0.id); stack1 = typeof stack1 === functionType ? stack1.call(depth0, {hash:{},data:data}) : stack1; }
  buffer += escapeExpression(stack1)
    + "\" data-youtube-id=\"";
  if (stack1 = helpers.youtubeID) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = (depth0 && depth0.youtubeID); stack1 = typeof stack1 === functionType ? stack1.call(depth0, {hash:{},data:data}) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n		<td class=\"votes ";
  options = {hash:{},data:data};
  buffer += escapeExpression(((stack1 = helpers.voted || (depth0 && depth0.voted)),stack1 ? stack1.call(depth0, (depth0 && depth0.id), options) : helperMissing.call(depth0, "voted", (depth0 && depth0.id), options)))
    + "\">\n		<a class=\"upvote\">▲</a><br>\n		";
  options = {hash:{},data:data};
  buffer += escapeExpression(((stack1 = helpers.limit || (depth0 && depth0.limit)),stack1 ? stack1.call(depth0, (depth0 && depth0.weight), options) : helperMissing.call(depth0, "limit", (depth0 && depth0.weight), options)))
    + "<br>\n		<a class=\"downvote\">▼</a>\n		</td>\n		<td><img src=\"";
  if (stack2 = helpers.thumbnail) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = (depth0 && depth0.thumbnail); stack2 = typeof stack2 === functionType ? stack2.call(depth0, {hash:{},data:data}) : stack2; }
  buffer += escapeExpression(stack2)
    + "\" /></td>\n		<td>\n			<header><a href=\"http://youtu.be/";
  if (stack2 = helpers.youtubeID) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = (depth0 && depth0.youtubeID); stack2 = typeof stack2 === functionType ? stack2.call(depth0, {hash:{},data:data}) : stack2; }
  buffer += escapeExpression(stack2)
    + "\"><h3>";
  if (stack2 = helpers.title) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = (depth0 && depth0.title); stack2 = typeof stack2 === functionType ? stack2.call(depth0, {hash:{},data:data}) : stack2; }
  buffer += escapeExpression(stack2)
    + "</h3></a><small class=\"cid-box\">";
  if (stack2 = helpers.cid) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = (depth0 && depth0.cid); stack2 = typeof stack2 === functionType ? stack2.call(depth0, {hash:{},data:data}) : stack2; }
  buffer += escapeExpression(stack2)
    + "</small></header>\n			<p>";
  options = {hash:{},data:data};
  buffer += escapeExpression(((stack1 = helpers.ntobr || (depth0 && depth0.ntobr)),stack1 ? stack1.call(depth0, (depth0 && depth0.description), options) : helperMissing.call(depth0, "ntobr", (depth0 && depth0.description), options)))
    + "</p>\n		</td>\n	</tr>\n";
  return buffer;
  }

  buffer += "<table>\n	<tbody>\n";
  stack1 = helpers.each.call(depth0, depth0, {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data});
  if(stack1 || stack1 === 0) { buffer += stack1; }
  buffer += "\n	</tbody>\n</table>";
  return buffer;
  });
})();