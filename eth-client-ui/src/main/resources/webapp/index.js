new Vue({
    el: '#app',
    data () {
        return {
            config: null
        }
    },
    mounted () {
        axios
            .get('/rest/config')
            .then(response => (this.config = response.data));
    }
})