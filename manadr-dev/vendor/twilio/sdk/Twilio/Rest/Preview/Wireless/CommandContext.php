<?php

/**
 * This code was generated by
 * \ / _    _  _|   _  _
 * | (_)\/(_)(_|\/| |(/_  v1.0.0
 * /       /
 */

namespace Twilio\Rest\Preview\Wireless;

use Twilio\InstanceContext;
use Twilio\Values;
use Twilio\Version;

class CommandContext extends InstanceContext {
    /**
     * Initialize the CommandContext
     * 
     * @param \Twilio\Version $version Version that contains the resource
     * @param string $sid The sid
     * @return \Twilio\Rest\Preview\Wireless\CommandContext 
     */
    public function __construct(Version $version, $sid) {
        parent::__construct($version);
        
        // Path Solution
        $this->solution = array(
            'sid' => $sid,
        );
        
        $this->uri = '/Commands/' . $sid . '';
    }

    /**
     * Fetch a CommandInstance
     * 
     * @return CommandInstance Fetched CommandInstance
     */
    public function fetch() {
        $params = Values::of(array());
        
        $payload = $this->version->fetch(
            'GET',
            $this->uri,
            $params
        );
        
        return new CommandInstance(
            $this->version,
            $payload,
            $this->solution['sid']
        );
    }

    /**
     * Provide a friendly representation
     * 
     * @return string Machine friendly representation
     */
    public function __toString() {
        $context = array();
        foreach ($this->solution as $key => $value) {
            $context[] = "$key=$value";
        }
        return '[Twilio.Preview.Wireless.CommandContext ' . implode(' ', $context) . ']';
    }
}