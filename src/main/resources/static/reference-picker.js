var template;
{
    var xhr = new XMLHttpRequest();
    xhr.open('GET', "reference-picker.html", false);
    xhr.onload = function () {
        template = xhr.responseText
    }
    xhr.send();
}


Vue.component('reference-picker', {
    template: template,
    props: {
        options: Object,
        data: Object,
        selection: Object
    },

    // data:function(){
    //
    //   return {selection: ''}
    // },
    watch: {
        selection: function(val){
            console.log(val);

            this.data[this.metadata.keyFieldDescriptor.name] = val;
        }

    },

    data: function(){

        var java = this.options.class;

        var xhr = new XMLHttpRequest();
        var self = this
        xhr.open('GET', "http://localhost:8080/classdefinition?className=" + java, false);
        xhr.setRequestHeader("access_token", localStorage['access_token']);

        var metadata;
        xhr.onload = function () {
            metadata = JSON.parse(xhr.responseText)

            for(var idx in metadata.fieldDescriptors){

                var fd = metadata.fieldDescriptors[idx];

                if(fd.attributes && fd.attributes['namefield']){
                    metadata['nameFieldDescriptor'] = fd;
                }

            }

        }
        xhr.send();


        var rowData;
        var pathElements = java.split(".");
        var path = pathElements[pathElements.length-1].toLowerCase();
        var xhr = new XMLHttpRequest()
        var self = this

        xhr.open('GET', "http://localhost:8080/" + path, false);
        xhr.setRequestHeader("access_token", localStorage['access_token']);

        xhr.onload = function () {
            var jsonData = JSON.parse(xhr.responseText)
            rowData = jsonData._embedded[path];
        }
        xhr.send();


        return {
            java: java,
            metadata: metadata,
            rowData: rowData
        }
    },

    methods:{
        showLabel: function(entry){
            if(this.metadata.nameFieldDescriptor){
                return entry[this.metadata.nameFieldDescriptor.name];
            }else{
                return entry;
            }
        },
        keyValue: function(entry){

            return entry[this.metadata.keyFieldDescriptor.name];

        }
    }

})

