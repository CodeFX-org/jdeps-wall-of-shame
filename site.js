
$.extend($.easing,
{
    def: 'easeOutQuad',
    easeInOutExpo: function (x, t, b, c, d) {
        if (t==0) return b;
        if (t==d) return b+c;
        if ((t/=d/2) < 1) return c/2 * Math.pow(2, 10 * (t - 1)) + b;
        return c/2 * (-Math.pow(2, -10 * --t) + 2) + b;
    }
});

window.navScrollerDefaultSettings = {
	scrollToOffset: 170,
	scrollSpeed: 800,
	activateParentNode: true,
};

(function( $ ) {

    var settings;
    var disableScrollFn = false;
    var navItems;
    var navs = {}, sections = {};

    $.fn.navScroller = function(options) {
        settings = $.extend(window.navScrollerDefaultSettings, options );
        navItems = this;

        //attatch click listeners
    	navItems.on('click', function(event){
    		event.preventDefault();
            var navID = $(this).attr("href").substring(1);
            disableScrollFn = true;
            activateNav(navID);
            populateDestinations(); //recalculate these!
        	$('html,body').animate({scrollTop: sections[navID] - settings.scrollToOffset},
                settings.scrollSpeed, "easeInOutExpo", function(){
                    disableScrollFn = false;
                }
            );
    	});

        //populate lookup of clicable elements and destination sections
        populateDestinations(); //should also be run on browser resize, btw

        // setup scroll listener
        $(document).scroll(function(){
            if (disableScrollFn) { return; }
            var page_height = $(window).height();
            var pos = $(this).scrollTop();
            for (i in sections) {
                if ((pos + settings.scrollToOffset >= sections[i]) && sections[i] < pos + page_height){
                    activateNav(i);
                }
            }
        });
    };

    function populateDestinations() {
        navItems.each(function(){
            var scrollID = $(this).attr('href').substring(1);
            navs[scrollID] = (settings.activateParentNode)? this.parentNode : this;
            sections[scrollID] = $(document.getElementById(scrollID)).offset().top;
        });
    }

    function activateNav(navID) {
        for (nav in navs) { $(navs[nav]).removeClass('active'); }
        $(navs[navID]).addClass('active');
    }
})( jQuery );


$(document).ready(function (){

    $('nav li a').navScroller();

    //section divider icon click gently scrolls to reveal the section
	$(".sectiondivider").on('click', function(event) {
    	$('html,body').animate({scrollTop: $(event.target.parentNode).offset().top - 50}, 400, "linear");
	});

    //links going to other sections nicely scroll
	$(".container a").each(function(){
        if ($(this).attr("href").charAt(0) == '#'){
            $(this).on('click', function(event) {
        		event.preventDefault();
                var target = $(event.target).closest("a");
                var targetHight =  $(target.attr("href")).offset().top
            	$('html,body').animate({scrollTop: targetHight - 170}, 800, "easeInOutExpo");
            });
        }
	});

	// Additions by jki & nipa

	// we'd like to have links between artifacts;
	// in order to keep the (already humonguous) HTML from getting even bigger,
	// we add them vie JS after the side was loaded

	// add anchors to the dependants
	$('.dt').each(function (i, th) {
		$(th).html('<a name="' + th.textContent + '">' + th.textContent + '</a>');
	});

	// add links to the dependees
	$('.de').each(function (i, td) {
		$(td).html('<a href="#' + td.textContent.replace(/ : /g, ":") + '">' + td.textContent + '</a>');
	});

	// add a global listener that scrolls to the anchor
	$(document.body).on('click', '.de a', function (event) {
		scrollToId($(event.target).attr('href').substr(1));
	});

	scrollToId(location.hash.substr(1));

	function scrollToId(id) {
		var settings = window.navScrollerDefaultSettings;
		var $target = $('[name="' + id + '"]');

		if (! $target.is(':empty')) {
			$('html,body').animate(
				{ scrollTop: $target.offset().top - settings.scrollToOffset },
				settings.scrollSpeed, "easeInOutExpo", function () {
					$target.parents('.artifacts').fadeOut().fadeIn();
				});
		}
	}

});
