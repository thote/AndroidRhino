var add = require("./add").add;
exports.subtract = function(a, b) {
  return add(a, -b);
};