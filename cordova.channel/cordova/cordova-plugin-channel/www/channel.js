cordova.define("cordova-plugin-channel.Channel", function(require, exports, module) {

function Channel() {
  this.callbacks = {};
}

Channel.prototype.newTask = function() {
  let task = null;
  if (this.task) {
    task = this.task + 1;
  } else {
    task = Math.floor(Math.random() * 2000000000);
  }
  this.task = task;
  task = '' + task;
  return task;
};

Channel.prototype.exec = function(scheme, data, onNext, onError, onComplete) {
  let task = this.newTask();
  this.callbacks[task] = {
    "onNext": onNext,
    "onError": onError,
    "onComplete": onComplete
  };

  let execTimeout;
  if (arguments.length == 6) {
    try {
      execTimeout = setTimeout(() => {
        try {
          if (this.callbacks[task]) {
            delete this.callbacks[task];
            if (onError) {
              onError({
                'ERROR_MSG': 'exec timeout'
              });
            }
          }
        } catch (e) {
          console.error(e);
        }
      }, arguments[5]);
    } catch (e) {
        console.error(e);
    }
  }

  let _this = this;
  cordova.exec(function(data) {
    if(execTimeout) {
      clearTimeout(execTimeout);
    }
    try {
      if (_this.callbacks[task]) {
        if(data["__channel__keep__"] && _this.callbacks[task].onNext) {
            _this.callbacks[task].onNext(data);
        }else if(_this.callbacks[task].onComplete) {
            _this.callbacks[task].onComplete();
            delete _this.callbacks[task];
        }
      }
    } catch (e) {
      console.error(e);
    }
  }, function(data) {
    if(execTimeout) {
      clearTimeout(execTimeout);
    }
    try {
      if (_this.callbacks[task] && _this.callbacks[task].onError) {
        _this.callbacks[task].onError(data);
        delete _this.callbacks[task];
      }
    } catch (e) {
      console.error(e);
    }
  }, "Channel", "exec", [scheme, data, task]);

  return task;
};

Channel.prototype.cancel = function(task) {
  if (this.callbacks[task]) {
    delete this.callbacks[task];
  }
  cordova.exec(null, null, "Channel", "cancel", [task])
};

Channel.prototype.subscribe = function(onNext, onError) {
  let task = this.newTask();
  this.callbacks[task] = {
    "onNext": onNext,
    "onError": onError
  };
  cordova.exec(onNext, onError, "Channel", "subscribe", [task]);
  return task;
};

Channel.prototype.dispose = function(task) {
  cordova.exec(null, null, "Channel", "dispose", [task]);
};

Channel.prototype.register = function(tag, onNext, onError) {
  let task = this.newTask();
  this.callbacks[task] = {
    "onNext": onNext,
    "onError": onError
  };
  cordova.exec(onNext, onError, "Channel", "register", [tag, task]);
  return task;
};

Channel.prototype.unregister = function(tag, task) {
  cordova.exec(null, null, "Channel", "unregister", [tag, task]);
};

module.exports = new Channel();

});