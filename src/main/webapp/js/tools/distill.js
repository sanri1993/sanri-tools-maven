define(['util'],function (util) {
    var distill = {};
    var regexs = {
        'java属性':{pattern:/(private|protected)\s+\w+\s+(\w+);/,index:2},
        'java 属性值':{pattern:/(private|protected|public).*?=\s*(.+?);/,index:2},
        'java 公共属性':{pattern:/public.*?\s(\w+)\s=/,index:1},
        '文档注释':{pattern:/\* (.*?)\n/,index:1},
        '属性列':{pattern: /property="(\w+)"/,index:1},
    '数据库列':{pattern:/column="(\w+)"/,index:1},
        'temp':{pattern:/\w\.(\w+),/,index:1},
        '表列':{pattern:/"(\w+)"\s.*/,index:1},
        '表列类型':{pattern:/"(\w+)"\s(\w+).*/,index:2},
        '表列注释':{pattern:/\'(.+)\';/,index:1},
        'hibernatesql':{pattern:/"(.+)"/,index:1},
        'aliassql':{pattern:/(\w+),/,index:1},
        '注释提取':{pattern:/value\s+=\s+"(.+)"/,index:1},
        '类型提取':{pattern:/(private|protected)\s+(\w+)/,index:2}
    };

    distill.init = function () {
        bindEvents();
        $('textarea[autoHeight]').autoHeight();

        var $regexs = $('#examples>ul.dropdown-menu').empty();
        for (var key in regexs) {
            var $item = $('<li name="' + key + '"><a href="javascript:void(0);">' + key + '</a></li>').appendTo($regexs);
        }
         $('#examples>.dropdown-menu>li:first').click();
        distill.regex = regexs['java属性'];
        $('#examples>.dropdown-menu').dropdown('toggle');

        return this;
    };

    function bindEvents() {
        var events = [{parent:'#examples>.dropdown-menu',selector:'li',types:['click'],handler:switchRegex},
            {selector:'#distllbtn',types:['click'],handler:distillContent},
            {selector:'#a_b2aB',types:['click'],handler:a_b2aB},
            {selector:'#aB2a_b',types:['click'],handler:aB2a_b}];
        util.regPageEvents(events);

        /**
         * 下划线转驼峰
         */
        function a_b2aB() {
            var strings = clearSources();
            var newValues = [];
            for (var i=0;i<strings.length;i++){
                var line = strings[i];
                newValues.push(util.StringUtil._2aB(line));
            }
            $('#result').val(newValues.join('\n'));
        }

        /**
         * 驼峰转下划线
         */
        function aB2a_b() {
            var strings = clearSources();
            var newValues = [];
            for (var i=0;i<strings.length;i++){
                var line = strings[i];
                newValues.push(util.StringUtil.aB2_(line).toLowerCase());
            }
            $('#result').val(newValues.join('\n'));
        }

        /**
         * 清理原数据,对每行去前后空格,去空行
         */
        function clearSources() {
            var source = $('#source').val().trim();
            var strings = source.split('\n');
            var newstrings = [];
            for (var i=0;i<strings.length;i++){
                var line = strings[i].trim();
                if(!line){
                    continue;
                }

                newstrings.push(line.trim());
            }
            $('#source').val(newstrings.join('\n'));
            return newstrings;
        }

        function distillContent() {
             var strings = clearSources();
            var results = [];
            for(var i=0;i<strings.length;i++){
                var line = strings[i].trim();
                var regExpExecArray = distill.regex.pattern.exec(line);
                if(regExpExecArray) {
                    results.push(regExpExecArray[distill.regex.index]);
                }
            }
            $('#result').val(results.join('\n'));
        }

        function switchRegex() {
            var key = $(this).attr('name');
            $('#examples>button>span:eq(0)').text(key);
            $('#examples').siblings('input').val(regexs[key].pattern);
            distill.regex = regexs[key];
             $('#examples>.dropdown-menu').dropdown('toggle');
        }
    }

    return distill.init();
});