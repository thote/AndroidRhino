var add = require("./add");

exports.subtract = function(a, b) {
  return add(a, -b);
};