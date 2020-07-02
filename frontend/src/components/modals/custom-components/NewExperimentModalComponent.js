import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../shapes'
import Modal from '../Modal'

class NewExperimentModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        topologies: PropTypes.arrayOf(Shapes.Topology),
        schedulers: PropTypes.arrayOf(Shapes.Scheduler),
        traces: PropTypes.arrayOf(Shapes.Trace),
        callback: PropTypes.func.isRequired,
    }

    reset() {
        this.textInput.value = ''
        this.topologySelect.selectedIndex = 0
        this.traceSelect.selectedIndex = 0
        this.schedulerSelect.selectedIndex = 0
    }

    onSubmit() {
        this.props.callback(
            this.textInput.value,
            this.topologySelect.value,
            this.traceSelect.value,
            this.schedulerSelect.value,
        )
        this.reset()
    }

    onCancel() {
        this.props.callback(undefined)
        this.reset()
    }

    render() {
        return (
            <Modal
                title="New Experiment"
                show={this.props.show}
                onSubmit={this.onSubmit.bind(this)}
                onCancel={this.onCancel.bind(this)}
            >
                <form
                    onSubmit={e => {
                        e.preventDefault()
                        this.onSubmit()
                    }}
                >
                    <div className="form-group">
                        <label className="form-control-label">Name</label>
                        <input
                            type="text"
                            className="form-control"
                            required
                            ref={textInput => (this.textInput = textInput)}
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Topology</label>
                        <select
                            className="form-control"
                            ref={topologySelect => (this.topologySelect = topologySelect)}
                        >
                            {this.props.topologies.map(topology => (
                                <option value={topology._id} key={topology._id}>
                                    {topology.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Trace</label>
                        <select
                            className="form-control"
                            ref={traceSelect => (this.traceSelect = traceSelect)}
                        >
                            {this.props.traces.map(trace => (
                                <option value={trace._id} key={trace._id}>
                                    {trace.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Scheduler</label>
                        <select
                            className="form-control"
                            ref={schedulerSelect => (this.schedulerSelect = schedulerSelect)}
                        >
                            {this.props.schedulers.map(scheduler => (
                                <option value={scheduler.name} key={scheduler.name}>
                                    {scheduler.name}
                                </option>
                            ))}
                        </select>
                    </div>
                </form>
            </Modal>
        )
    }
}

export default NewExperimentModalComponent
