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
	scrollToOffset: 210,
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
    	$('html,body').animate({scrollTop: $(event.target.parentNode).offset().top - 80}, 400, "linear");
	});

	// add a global listener that handles link clicks
	$(document.body).on('click', 'a', scrollIfInternalLink);

	function scrollIfInternalLink(event) {
		var href = $(event.target).attr("href");
		if (href[0] == '#') {
			// setting the hash in would be cool
 			// but it leads to instant hopping to the location
			// avoiding our smooth scrolling
			// location.hash = href;
			event.preventDefault();
			var id = href.substr(1);
			if (linksToArtifact(id))
				scrollToArtifact(id);
			else
				scrollToSection(id);
		}
	}

	function linksToArtifact(id) {
		// artifact ids contain colons
		return id.indexOf(':') != -1;
	}

	function scrollToArtifact(id) {
		// don't use `$(id)` - ids contain colons and jQuery doesn't like them
		var element =  $(document.getElementById(id));
		scrollTo(element).then(function () {
			element.parents('.artifacts').fadeOut().fadeIn().fadeOut().fadeIn();
		});
	}

	function scrollToSection(id) {
		var element =  $(document.getElementById(id));
		scrollTo(element);
	}

	function scrollTo(element) {
		var settings = window.navScrollerDefaultSettings;
		var targetHeight =  element.offset().top;
		return $('html,body').animate(
			{ scrollTop: targetHeight - settings.scrollToOffset },
			settings.scrollSpeed,
			"easeInOutExpo").promise();
	}

});