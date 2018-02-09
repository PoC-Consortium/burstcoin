# For Emacs: -*- mode:cperl; eval: (folding-mode 1); coding:utf-8; -*-
package BURST::API::RequestTypes;

# {{{ use block

use warnings;
use strict;
use utf8;
use feature ':5.12';

use Data::Dumper;
use Data::Rx;
use JSON                           qw(-support_by_pp);           # need JSON to support bignum
use LWP::UserAgent;
use LWP::Protocol::https;

use Test::More;

# }}}
# {{{ var block

our @ISA = qw(Exporter);
our @EXPORT = qw(loop_reqtypes
            );

my $rx = Data::Rx->new;
my $ua      = LWP::UserAgent->new(
#    ssl_opts => { verify_hostname => 1 },
);

my $URLBASE = 'http://localhost:8125/burst?requestType=';

my %config = (
    server_url     => $URLBASE,
    max_retries => 3,
);


# skip: default 0 (if to skip that test)
# type: default GET
# args: default none/undef
our $reqtypes = [ # we want to define a sequence of tests
    # {{{ getAllAssets
    {
        name => 'getAllAssets',
        required => {
            assets => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenAskOrders
    {
        name => 'getAllOpenAskOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAllOpenBidOrders
    {
        name => 'getAllOpenBidOrders',
        required => {
            openOrders => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getAssetIds
    {
        name => 'getAssetIds',
        required => {
            assetIds => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getATIds
    {
        name => 'getATIds',
        required => {
            atIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },

    },
    # }}}
    # {{{ getBlock
    {
        name => 'getBlock',
        required => {
            previousBlockHash => '//str',
            payloadLength => '//int',
            totalAmountNQT => '//int',
            generationSignature => '//str',
            generator => '//int',
            generatorPublicKey => '//str',
            baseTarget => '//int',
            payloadHash => '//str',
            generatorRS => '//str',
            blockReward => '//int',
            scoopNum => '//int',
            numberOfTransactions => '//int',
            blockSignature => '//str',
            transactions => {
                type => '//arr',
                contents => '//int',
            },
            nonce => '//int',
            version => '//int',
            totalFeeNQT => '//int',
            previousBlock => '//int',
            block => '//int',
            height => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getBlockchainStatus
    {
        name => 'getBlockchainStatus',
        required => {
            lastBlock => '//int',
            application => {
                type => '//str',
                value => "BRS",
            },
            time       => '//int',
            version    => '//str',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfBlocks => '//int',
            lastBlockchainFeeder => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getBlocks
    {
        name => 'getBlocks',
        required => {
            blocks => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getConstants
    {
        skip => 1,
        name => 'getConstants',
        required => {
            maxBlockPayloadLength => {
                type => '//int',
                value => 44880,
            },
            maxArbitraryMessageLength => {
                type => '//int',
                value => 1000,
            },
            genesisBlockId => {
                type => '//int',
                value => 3444294670862540038,
            },
            "transactionTypes" => '//arr',
        },
    },
    # }}}
    # {{{ getDGSGoods
    {
        name => 'getDGSGoods',
        required => {
            good => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getDGSPurchases
    {
        name => 'getDGSPurchases',
        required => {
            purchases => {
                type => '//arr',
                contents => '//any',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getECBlock
    {
        name => 'getECBlock',
        required => {
            ecBlockHeight => '//int',
            ecBlockId => '//int',
            timestamp => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getMiningInfo
    {
        name => 'getMiningInfo',
        required => {
            baseTarget   => '//int',
            height => '//int',
            generationSignature => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getMyInfo
    {
        name => 'getMyInfo',
        required => {
            address => '//str',
            host    => '//str',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getPeers
    {
        name => 'getPeers',
        required => {
            peers => {
                type     => '//arr',
                contents => '//str',
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getState

    {
        name => 'getState',
        required => {
            numberOfPeers => '//int',
            numberOfUnlockedAccounts => '//int',
            numberOfTransfers => '//int',
            numberOfOrders => '//int',
            numberOfTransactions => '//int',
            maxMemory => '//int',
            isScanning => '//bool',
            cumulativeDifficulty => '//int',
            numberOfAssets => '//int',
            freeMemory => '//int',
            availableProcessors => '//int',
            totalEffectiveBalanceNXT => '//int',
            numberOfAccounts => '//int',
            numberOfBlocks => '//int',
            version => '//str',
            numberOfBidOrders => '//int',
            lastBlock => '//int',
            totalMemory => '//int',
            application => '//str',
            numberOfAliases => '//int',
            lastBlockchainFeederHeight => '//int',
            numberOfTrades => '//int',
            time => '//int',
            numberOfAskOrders => '//int',
            lastBlockchainFeeder => '//str',
            requestProcessingTime => '//int',
        },
    },

    # }}}
    # {{{ getTime
    {
        name => 'getTime',
        txt  => 'get wallet time in seconds since genesis block',
        required => {
            time => '//int',
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getUnconfirmedTransactionIds
    {
        name => 'getUnconfirmedTransactionIds',
        debug => 1,
        required => {
            unconfirmedTransactionIds => {
                type => '//arr',
                contents => '//int'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ getUnconfirmedTransactions
    {
        name     => 'getUnconfirmedTransactions',
        required => {
            unconfirmedTransactions => {
                type => '//arr',
                contents => '//any'
            },
            requestProcessingTime => '//int',
        },
    },
    # }}}
    # {{{ longConvert
    {
        name => 'longConvert',
        required => {
            stringId => {
                type => '//int',
                value => 100,
            },
            longId => {
                type => '//int',
                value => 100,
            },
            requestProcessingTime => '//int',
        },
        args => {
            id => 100,
        },
    },
    # }}}
    # {{{ rsConvert
    {
        name => 'rsConvert',
        txt  => 'convert numeric ID to RS address',
        required => {
            accountRS => {
                type => '//str',
                value => "BURST-EMXC-3PFB-J8HQ-GJ9HZ",
            },
            account => {
                    type => '//int',
                    value => 17274946210831421354,
                },
            requestProcessingTime => '//int',
        },
        args => {
            account => 17274946210831421354,
        },
    },
    # }}}
];

# }}}

# {{{ loop_reqtypes

sub loop_reqtypes {
    my $tests = 0;

  LOOP_REQS:
    for my $rtype (@{$reqtypes}) {
        next LOOP_REQS if ($rtype->{skip});

        my $rt_name   = $rtype->{name};
        my $rt_schema = build_schema($rtype);
        my $rt_args   = $rtype->{args};
        my $reply     = talk2wallet($rt_name, $rt_args);

        print Dumper($reply) if $rtype->{debug};

        my $valid   = $rt_schema->check($reply);
        my $testtxt = $rt_name                             # build test text
                    . ($rtype->{txt} ? " - $$rtype{txt}"   # with more than just req name
                                     : '')                 # if available
                                     ;
        ok($valid, $testtxt);
        $tests++;

        if (!$valid) {
            print Dumper($reply);
            my $rx_failure = $rt_schema->assert_valid($reply);
            print Dumper($rx_failure);
        }
    }

    return $tests;
}

# }}}

# {{{ build_schema

sub build_schema {
    my $rtype = shift;
    my $schema = {
        type     => '//rec',
        required => $rtype->{required},
    };

    #print Dumper($schema);
    return $rx->make_schema($schema);
}

# }}}

# {{{ talk2wallet                  talk to the wallet

sub talk2wallet {
    my $path    = shift;                # relative URL on server
    my $send    = shift;                # data to send to server
    my $verbose = shift // 1;           # do we want status reports (default: yes)

    my $json    = JSON->new->utf8->allow_blessed->allow_bignum;       # create JSON object with features
    my $request;

    if (defined $send) {
#    print "$config{server_url}$path\n";

    #     my $content = $json->encode($send);                               # convert Perl structure to JSON
    #     my $header  = HTTP::Headers->new(                                 # construct HTTP header
    #         Content_Length => length($content),
    #         Content_Type   => 'application/json;charset=utf-8'
    #     );
    #     $request = HTTP::Request->new('POST',
    #                                   "$config{server_url}$path",
    #                                   $header,
    #                                   $content);
    # }
    # else {
        $path .= ('&' . hash2get($send));
    }


    
    $request = HTTP::Request->new('GET',
                                  "$config{server_url}$path");

    my $retries  = $config{max_retries};                    # number of retries in case the server does not answer
    my $response = _get_srv_response($request, $verbose);   # hold the response from the server.

    return $json->decode($response);                        # decode answer to Perl structure
}

# }}}
# {{{ _get_srv_response            get response from server given request and retries

sub _get_srv_response {
    my $request = shift;
    my $verbose = shift // 1;

    my $retries = $config{max_retries};      # number of retries in case the server does not answer
    my $response;                            # hold the response from the server.

  SRVCON_LOOP:
    while ($retries--) {                                         # prepare to have to retry connecting to server
        $response = $ua->request($request);                      # get answer

        last SRVCON_LOOP if ($response->is_success);        # end connect loop if all ok

        # HERE: Problems connecting to server. So retry.
        my $status = $response->status_line;                     # get status line message
        _out_unbuffered("\nProblem connecting to server " . $request->uri . "(status: $status). Retries left: $retries\n", $verbose);
        cleanup_end($status) if (!$retries);                     # end program with status line if out of retries
        my $sleep = sprintf("%2.3f", 5 * ($config{max_retries} - $retries) + rand(20));  # get sleep length
        _out_unbuffered("Sleeping ${sleep} s...\n", $verbose);   # inform user about sleep
        sleep $sleep;                                            # sleep, then retry
    }

    return $response->content;
}

# }}}

# {{{ hash2get

sub hash2get {
    my $href = shift;

    my $str = join '&', map { "$_=$$href{$_}" } (keys %{$href});

    #print "STR: $str\n";

    return $str;
}

# }}}
# {{{ cleanup & end                cleanup & end / graceful termination

sub cleanup_end {
    my $msg = shift;

    if (defined $msg) {
        _out_unbuffered("$msg\n");
    }
    ReadMode('normal') if ($^O ne 'MSWin32');
#    $pm->finish()      if (defined $pm && $cpus > 1);

    exit 0;
}

# }}}
# {{{ _out_unbuffered              unbuffered output

sub _out_unbuffered {
    my $str  = shift;         # message to show
    my $show = shift // 1;    # do we actually want it to be shown (default: yes)

    $| = 1;                   # unbuffered STDOUT (prints do not wait for newline)
    print $str if($show);     # do the print if show is on
    $| = 0;                   # restore to buffered STDOUT

    return;
}

# }}}


return 1;
