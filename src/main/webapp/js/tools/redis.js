define(['util','dialog','template'],function (util,dialog,template) {
    var redisclient = {};
    var apis = {
        connNames:'/file/manager/simpleConfigNames',
        createConn:'/file/manager/writeConfig',
        detail:'/file/manager/readConfig',
        serializes:'/zk/serializes'
    };
    var modul = 'redis';

    function initApis(){
        var methods = ['redisInfo','dbs','scan','readValue','readHashValue','batchDelete','hscan'];
        methods.forEach(function (method) {
            apis[method] = '/redis/'+method;
        });
    }

    redisclient.init = function () {
        initApis();
        loadSerializes();
        bindEvent();
        
        loadConns(function () {
            $('#connect>.dropdown-menu>li:first').click();
            $('#connect>.dropdown-menu').dropdown('toggle');
        });
        
        return this;
    }

    /**
     * 加载所有序列化工具
     */
    function loadSerializes() {
        util.requestData(apis.serializes,function (serializes) {
            var $serializes =  $('#datashow').find('select[name=deserialize]').empty();
            for(var i=0;i<serializes.length;i++){
                $serializes.append('<option value="'+serializes[i]+'">'+serializes[i]+'</option>')
            }
            //选中第一个
            $serializes.val(serializes[0]);
        });
    }

    /**
     * 加载所有的连接
     * @param callback
     */
    function loadConns(callback) {
        util.requestData(apis.connNames,{modul:modul},function (conns) {
            var $menu = $('#connect>ul.dropdown-menu').empty();
            if(conns){
                for(var i=0;i<conns.length;i++){
                    var $item = $('<li name="'+conns[i]+'"><a href="javascript:void(0);">'+conns[i]+'</a></li>').appendTo($menu);
                    $item.data('value',conns[i]);
                }
                if(callback){
                    callback(conns);
                }
            }
        });
    }

    /**
     * 加载所有数据库
     */
    function loadDatabases() {
        //加载所有库信息
        util.requestData(apis.dbs,{name:redisclient.conn},function (dbs) {
            var htmlCode = template('tabnavTemplate',{dbs:dbs});
            $('#redisDataShow').html(htmlCode);

            //加载 redis 连接总信息
            util.requestData(apis.redisInfo,{name:redisclient.conn},function (infos) {
                // $('#redisDataShow').find('#info').html(info);
                var htmlCode = template('infoTemplate',{infos:infos});
                var $info = $('#redisDataShow').find('#info');
                $info.html(htmlCode);
                $info.find('li:first').addClass('active');
                $info.find('.tab-pane:first').addClass('active')
            });
        });
    }

    function readValue(index, key) {
        $('#datashow').find('p:first').text(index);
        $('#datashow').find('p:last').text(key);
        var deserialize = $('#datashow').find('select[name=deserialize]').val();
        util.requestData(apis.readValue,{name:redisclient.conn,index:index,key:key,serialize:deserialize},function (value) {
            $('#datashow').find('.data-value').text(value);
        });
    }

    function readHashValue(index, key) {
        $('#datashow').find('p:first').text(index);
        $('#datashow').find('p:last').text(key);
        var deserialize = $('#datashow').find('select[name=deserialize]').val();
        util.requestData(apis.hscan,{name:redisclient.conn,index:index,key:key,serialize:deserialize},function (value) {

        })
    }

    /**
     * 搜索和下一页合一； 初始搜索 cursor 写 0
     * @param $tabpane
     * @param cursor
     */
    function search($tabpane,cursor) {
        var index = $tabpane.attr('index');
        var dbSize = $tabpane.attr('dbSize');
        if(dbSize == '0')return ;

        var pattern = $tabpane.find('input').val().trim();
        util.requestData(apis.scan,{name:redisclient.conn,index:index,pattern:pattern,cursor:cursor,limit:10},function (keys) {
            var htmlCode = template('keysTemplate',{redisKeyResults:keys});
            $tabpane.find('tbody').html(htmlCode);
        });
    }
    
    function bindEvent() {
        var events = [{parent:'#connect>.dropdown-menu',selector:'li',types:['click'],handler:switchConn},
            {selector:'#newconnbtn',types:['click'],handler:newconn},
            {parent:'#redisDataShow',selector:'>ul>li>a[data-toggle="tab"]',types:['shown.bs.tab'],handler:showTab},
            {parent:'#redisDataShow',selector:'input',types:['keydown'],handler:keydownSearch},
            {parent:'#redisDataShow',selector:'button',types:['click'],handler:clickSearch},
            {parent:'#redisDataShow',selector:'tr',types:['click'],handler:clickTrReadValue},
            {selector:'#datashow select[name=deserialize]',types:['change'],handler:switchDeserialize}];
        
        util.regPageEvents(events);

        /**
         * 切换序列化工具
         */
        function switchDeserialize() {
            var index = $('#datashow').find('p:first').text();
            var key = $('#datashow').find('p:last').text();
            readValue(index,key);
        }

        /**
         * 点击表格行读取数据
         */
        function clickTrReadValue() {
            var $tabpane = $(this).closest('.tab-pane');
            var index = $tabpane.attr('index');
            var key = $(this).attr('key');
            var type = $(this).attr('type');
            if(type == 'hash'){
                readHashValue(index,key);
                return ;
            }
            readValue(index,key)
        }

        function keydownSearch(event) {
            var event = event || window.event;
            if(event.keyCode == 13) {
                var $tabpane = $(this).closest('.tab-pane');
                search($tabpane, 0);
            }
        }

        function clickSearch() {
            var $tabpane = $(this).closest('.tab-pane');
            search($tabpane,0);
        }

        /**
         * 显示对应数据库连接组件
         */
        function showTab(e) {
            var $target = $(e.target);
            var tabpaneId = $target.attr('href').substring(1);
            var $tabpane = $('#'+tabpaneId);

            if(tabpaneId != 'info'){
                search($tabpane,0);
            }
        }

        /**
         * 切换连接 
         */
        function switchConn() {
            var conn = $(this).data('value');
            redisclient.conn = conn;

            $('#connect>button>span:eq(0)').text(conn);
            util.requestData(apis.detail,{modul:modul,baseName:conn},function (address) {
                $('#connect').next('input').val(address);
            });
            $('#connect>.dropdown-menu').dropdown('toggle');

            loadDatabases();
        }

        /**
         * 新连接
         */
        function newconn() {
            dialog.create('新连接')
                .setContent($('#newconn'))
                .setWidthHeight('60%','300px')
                .addBtn({type:'yes',text:'确定',handler:function(index, layero){
                        var params = util.serialize2Json($('#newconn>form').serialize());
                        if(!params.name || !params.connectStrings){
                            layer.msg('请将信息填写完整');
                            return ;
                        }
                        params.modul = modul;
                        params.baseName = params.name;
                        params.content = JSON.stringify({connectStrings:params.connectStrings,auth:params.auth});
                        util.requestData(apis.createConn,params,function () {
                            layer.close(index);

                            loadConns(function (conns) {
                                if(conns){
                                    //请求最后一个连接,并选中
                                    $('#connect>.dropdown-menu>li[name='+params.name+']').click();
                                    $('#connect>.dropdown-menu').dropdown('toggle');
                                }
                            });
                        });
                    }})
                .build();
        }
    }

    return redisclient.init();
});